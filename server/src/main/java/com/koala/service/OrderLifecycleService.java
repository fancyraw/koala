package com.koala.service;

import com.koala.common.constant.ConfigKeys;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.entity.Order;
import com.koala.entity.OrderCoupon;
import com.koala.entity.OrderItem;
import com.koala.enums.OrderStatus;
import com.koala.repository.OrderCouponRepository;
import com.koala.repository.OrderItemRepository;
import com.koala.repository.OrderRepository;
import com.koala.repository.ProductSkuRepository;
import com.koala.repository.UserCouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单生命周期：C 端 cancel / confirm / delete，以及定时任务的超时取消 / 自动确认收货。
 * 释放资产（回滚库存 + 释放券）严格 CAS 先行，避免并发副作用。
 */
@Slf4j
@Service
public class OrderLifecycleService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderCouponRepository orderCouponRepository;
    private final ProductSkuRepository skuRepository;
    private final UserCouponRepository userCouponRepository;
    private final OrderQueryService orderQueryService;
    private final ConfigService configService;

    /** 自注入代理：autoCancelTimeout 循环内调用事务方法需走代理。 */
    @Autowired
    @Lazy
    private OrderLifecycleService self;

    public OrderLifecycleService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                                 OrderCouponRepository orderCouponRepository, ProductSkuRepository skuRepository,
                                 UserCouponRepository userCouponRepository, OrderQueryService orderQueryService,
                                 ConfigService configService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderCouponRepository = orderCouponRepository;
        this.skuRepository = skuRepository;
        this.userCouponRepository = userCouponRepository;
        this.orderQueryService = orderQueryService;
        this.configService = configService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, String orderNo) {
        Order order = orderQueryService.requireOwnedOrder(userId, orderNo);
        if (!OrderStatus.WAIT_PAY.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅待付款订单可取消");
        }
        releaseAssets(order, OrderStatus.CANCELED.code());
    }

    public void confirm(Long userId, String orderNo) {
        Order order = orderQueryService.requireOwnedOrder(userId, orderNo);
        if (OrderStatus.COMPLETED.is(order.getStatus())) {
            return; // 幂等
        }
        if (!OrderStatus.WAIT_RECEIVE.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅待收货订单可确认收货");
        }
        if (orderRepository.markCompletedCas(orderNo, LocalDateTime.now()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR);
        }
    }

    public void deleteByUser(Long userId, String orderNo) {
        Order order = orderQueryService.requireOwnedOrder(userId, orderNo);
        if (!(OrderStatus.COMPLETED.is(order.getStatus()) || OrderStatus.CANCELED.is(order.getStatus())
                || OrderStatus.REFUNDED.is(order.getStatus()))) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅已完成/已取消/已退款订单可删除");
        }
        orderRepository.markUserDeleted(orderNo);
    }

    public int autoCancelTimeout() {
        List<Order> timedOut = orderRepository.findTimedOutUnpaid(LocalDateTime.now());
        int count = 0;
        for (Order order : timedOut) {
            try {
                self.releaseTimedOut(order.getOrderNo());
                count++;
            } catch (Exception e) {
                log.error("超时取消订单失败: {}", order.getOrderNo(), e);
            }
        }
        return count;
    }

    /** 单订单超时释放(独立事务)：仅 status=0 时释放，CAS 防并发支付。 */
    @Transactional(rollbackFor = Exception.class)
    public void releaseTimedOut(String orderNo) {
        Order order = orderRepository.findByNo(orderNo);
        if (order == null || !OrderStatus.WAIT_PAY.is(order.getStatus())) {
            return;
        }
        releaseAssets(order, OrderStatus.CANCELED.code());
    }

    public int autoConfirmReceived() {
        int days = configService.getInt(ConfigKeys.Order.GROUP, ConfigKeys.Order.AUTO_CONFIRM_DAYS, 7);
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        List<Order> due = orderRepository.findAutoConfirmDue(deadline);
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Order order : due) {
            if (orderRepository.markCompletedCas(order.getOrderNo(), now) > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 释放资产(6.4)：CAS 先行——待付款(0)→targetStatus 成功后才回滚库存 & 释放券，
     * 避免与支付回调 / 定时超时任务并发时出现资产双倍释放。
     */
    private void releaseAssets(Order order, int targetStatus) {
        int affected = orderRepository.cancelFromUnpaidCas(
                order.getOrderNo(), targetStatus, LocalDateTime.now());
        if (affected == 0) {
            log.info("releaseAssets 跳过：订单状态已流转 orderNo={}", order.getOrderNo());
            return;
        }
        for (OrderItem item : orderItemRepository.findByOrderNo(order.getOrderNo())) {
            skuRepository.addStock(item.getSkuId(), item.getQuantity());
        }
        for (OrderCoupon oc : orderCouponRepository.findByOrderNo(order.getOrderNo())) {
            userCouponRepository.releaseLock(oc.getUserCouponId());
        }
    }
}
