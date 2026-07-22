package com.koala.service;

import com.koala.common.constant.ConfigKeys;
import com.koala.common.constant.PaymentChannels;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.entity.Order;
import com.koala.entity.OrderCoupon;
import com.koala.enums.OrderStatus;
import com.koala.event.OrderPaidEvent;
import com.koala.infra.pay.NotifyResult;
import com.koala.infra.pay.PaymentChannel;
import com.koala.infra.pay.PaymentChannelFactory;
import com.koala.infra.pay.PrepayCommand;
import com.koala.infra.pay.PrepayResult;
import com.koala.repository.CouponRepository;
import com.koala.repository.OrderCouponRepository;
import com.koala.repository.OrderRepository;
import com.koala.repository.PaymentRepository;
import com.koala.repository.UserCouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 订单支付：pay 生成 prepay 参数，payNotify 走渠道验签 + 事务化 handlePaid（CAS 置已付款、核销券、发事件）。
 * 与超时取消 race 时 handlePaid 会登记待人工退款，避免钱丢失。
 */
@Slf4j
@Service
public class OrderPaymentService {

    private final OrderRepository orderRepository;
    private final OrderCouponRepository orderCouponRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final OrderQueryService orderQueryService;
    private final ConfigService configService;
    private final PaymentChannelFactory paymentChannelFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** 自注入代理：验签 -> 事务方法 handlePaid 需走代理。 */
    @Autowired
    @Lazy
    private OrderPaymentService self;

    public OrderPaymentService(OrderRepository orderRepository, OrderCouponRepository orderCouponRepository,
                               PaymentRepository paymentRepository, CouponRepository couponRepository,
                               UserCouponRepository userCouponRepository, OrderQueryService orderQueryService,
                               ConfigService configService, PaymentChannelFactory paymentChannelFactory,
                               ApplicationEventPublisher eventPublisher, Clock clock) {
        this.orderRepository = orderRepository;
        this.orderCouponRepository = orderCouponRepository;
        this.paymentRepository = paymentRepository;
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.orderQueryService = orderQueryService;
        this.configService = configService;
        this.paymentChannelFactory = paymentChannelFactory;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public PrepayResult pay(Long userId, String orderNo) {
        Order order = orderQueryService.requireOwnedOrder(userId, orderNo);
        if (!OrderStatus.WAIT_PAY.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单不可支付");
        }
        if (order.getExpireAt() != null && order.getExpireAt().isBefore(LocalDateTime.now(clock))) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单已超时");
        }
        // 零元订单在提交事务里已直接置为 WAIT_SHIP，正常不会到这里；保底再拒绝一次。
        if (order.getPayAmount() != null && order.getPayAmount().signum() == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "零元订单无需支付");
        }
        PaymentChannel channel = paymentChannelFactory.get(
                configService.get(ConfigKeys.Payment.GROUP, ConfigKeys.Payment.ACTIVE_CHANNEL, PaymentChannels.WECHAT));
        PrepayCommand cmd = new PrepayCommand();
        cmd.setOrderNo(orderNo);
        cmd.setPayAmount(order.getPayAmount());
        cmd.setDescription("订单 " + orderNo);
        return channel.prepay(cmd);
    }

    public boolean payNotify(HttpServletRequest request) {
        PaymentChannel channel = paymentChannelFactory.get(
                configService.get(ConfigKeys.Payment.GROUP, ConfigKeys.Payment.ACTIVE_CHANNEL, PaymentChannels.WECHAT));
        NotifyResult notify = channel.parseNotify(request);
        if (notify == null || !notify.isSuccess() || notify.getOrderNo() == null) {
            log.warn("支付回调验签失败或未成功: {}", notify);
            return false;
        }
        return self.handlePaid(notify);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaid(NotifyResult notify) {
        String orderNo = notify.getOrderNo();
        Order order = orderRepository.findByNo(orderNo);
        if (order == null) {
            return false;
        }
        // 幂等：非待付款即视为已处理
        if (!OrderStatus.WAIT_PAY.is(order.getStatus())) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        // order 0→1 CAS
        if (orderRepository.markPaidCas(orderNo, now) == 0) {
            // 与超时取消 race：重新读订单，若已被取消而钱到账则登记待退款，事后由 admin/售后触发实际退款。
            Order latest = orderRepository.findByNo(orderNo);
            if (latest != null && OrderStatus.CANCELED.is(latest.getStatus())) {
                paymentRepository.markSuccess(orderNo, notify.getTransactionId(), now);
                log.error("订单已取消但收到成功支付回调，需人工介入退款: orderNo={}, txId={}, amount={}",
                        orderNo, notify.getTransactionId(), notify.getPayAmount());
                return true;
            }
            return true; // 已被其他节点处理
        }
        // 券 3→1 核销 + coupon.used_count+1
        for (OrderCoupon oc : orderCouponRepository.findByOrderNo(orderNo)) {
            if (userCouponRepository.redeem(oc.getUserCouponId(), now) > 0) {
                couponRepository.incrementUsedCount(oc.getCouponId());
            }
        }
        // payment 置成功(回调交易号)
        paymentRepository.markSuccess(orderNo, notify.getTransactionId(), now);
        eventPublisher.publishEvent(new OrderPaidEvent(this, orderNo));
        return true;
    }
}
