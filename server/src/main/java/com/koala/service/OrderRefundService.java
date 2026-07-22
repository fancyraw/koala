package com.koala.service;

import com.koala.common.constant.ConfigKeys;
import com.koala.common.constant.PaymentChannels;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.order.OrderRefundRequest;
import com.koala.entity.Order;
import com.koala.entity.OrderCoupon;
import com.koala.entity.Payment;
import com.koala.enums.OrderStatus;
import com.koala.enums.PaymentStatus;
import com.koala.event.RefundedEvent;
import com.koala.infra.pay.PaymentChannel;
import com.koala.infra.pay.PaymentChannelFactory;
import com.koala.infra.pay.RefundCommand;
import com.koala.infra.pay.RefundResult;
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

import java.time.LocalDateTime;

/**
 * 订单退款：admin 手动退款、售后通道退款。
 * 关键设计：CAS 先行拿到状态独占权 → 无事务调外部渠道 → 独立事务收尾（payment / 券 / order 5→6）。
 * 渠道失败会 CAS 回滚（若上游给出 rollbackStatus），否则由售后侧处理。
 */
@Slf4j
@Service
public class OrderRefundService {

    private final OrderRepository orderRepository;
    private final OrderCouponRepository orderCouponRepository;
    private final PaymentRepository paymentRepository;
    private final UserCouponRepository userCouponRepository;
    private final ConfigService configService;
    private final PaymentChannelFactory paymentChannelFactory;
    private final ApplicationEventPublisher eventPublisher;

    /** 自注入代理：channel 后的 finalizeRefund 需事务，走代理。 */
    @Autowired
    @Lazy
    private OrderRefundService self;

    public OrderRefundService(OrderRepository orderRepository, OrderCouponRepository orderCouponRepository,
                              PaymentRepository paymentRepository, UserCouponRepository userCouponRepository,
                              ConfigService configService, PaymentChannelFactory paymentChannelFactory,
                              ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderCouponRepository = orderCouponRepository;
        this.paymentRepository = paymentRepository;
        this.userCouponRepository = userCouponRepository;
        this.configService = configService;
        this.paymentChannelFactory = paymentChannelFactory;
        this.eventPublisher = eventPublisher;
    }

    public void adminRefund(OrderRefundRequest req) {
        Order order = orderRepository.findByNo(req.getNo());
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!OrderStatus.COMPLETED.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅已完成订单可手动退款");
        }
        // 3→5 售后中：CAS 拿到独占权，返回 0 说明并发已进入售后流程。
        if (orderRepository.updateStatusCas(req.getNo(),
                OrderStatus.COMPLETED.code(), OrderStatus.AFTER_SALE.code()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单状态已变更，请刷新重试");
        }
        // CAS 成功后 order 已 == AFTER_SALE，reload 一次以获取最新 payment 关联字段。
        Order latest = orderRepository.findByNo(req.getNo());
        executeRefund(latest, req.getReason(), OrderStatus.COMPLETED.code());
    }

    public void refundForAfterSale(String orderNo, String reason) {
        Order order = orderRepository.findByNo(orderNo);
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!OrderStatus.AFTER_SALE.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单不在售后中，不可退款");
        }
        // 售后侧已保证订单处于 AFTER_SALE；直接进入 CAS + channel + tx-B 流程。
        // rollbackStatus 传 -1 表示外部渠道失败时不做订单状态回滚（保持 AFTER_SALE，由售后侧处理）。
        executeRefund(order, reason, -1);
    }

    /**
     * 退款处理(6.5)：
     * 1) 调外部支付渠道（无事务，避免持锁跨网络）
     * 2) 事务 B 内更新 payment.refunded + 未过期券回退 + order 5→6 + 发事件
     * 渠道失败：如果 rollbackStatus &gt;= 0，把订单 CAS 回滚到该状态；否则保持 AFTER_SALE 由上游处理。
     */
    private void executeRefund(Order order, String reason, int rollbackStatus) {
        Payment payment = paymentRepository.findByOrderNoAndStatus(
                order.getOrderNo(), PaymentStatus.SUCCESS.code());
        PaymentChannel channel = paymentChannelFactory.get(
                payment != null ? payment.getChannel() : configService.get(ConfigKeys.Payment.GROUP, ConfigKeys.Payment.ACTIVE_CHANNEL, PaymentChannels.WECHAT));
        RefundCommand cmd = new RefundCommand();
        cmd.setOrderNo(order.getOrderNo());
        cmd.setTransactionId(payment != null ? payment.getTransactionId() : null);
        cmd.setRefundAmount(order.getPayAmount());
        cmd.setTotalAmount(order.getPayAmount());
        cmd.setReason(reason);

        RefundResult result;
        try {
            result = channel.refund(cmd);
        } catch (RuntimeException e) {
            rollbackRefundStatus(order.getOrderNo(), rollbackStatus, "channel 抛出异常");
            throw e;
        }
        if (result == null || !result.isSuccess()) {
            rollbackRefundStatus(order.getOrderNo(), rollbackStatus, "channel 返回失败");
            throw new BizException(ErrorCode.REFUND_FAILED);
        }
        // 走一次独立事务完成本地状态收尾。渠道已成功，此处失败需要人工介入（不再回滚渠道退款）。
        self.finalizeRefund(order.getOrderNo(), payment != null ? payment.getId() : null);
    }

    private void rollbackRefundStatus(String orderNo, int rollbackStatus, String cause) {
        if (rollbackStatus < 0) {
            return;
        }
        int affected = orderRepository.updateStatusCas(orderNo,
                OrderStatus.AFTER_SALE.code(), rollbackStatus);
        if (affected == 0) {
            log.warn("退款渠道失败但订单状态已流转，回滚跳过 orderNo={}, cause={}", orderNo, cause);
        }
    }

    /** 事务 B：本地状态收尾。channel 已成功，此处失败需人工修复；不再回滚。 */
    @Transactional(rollbackFor = Exception.class)
    public void finalizeRefund(String orderNo, Long paymentId) {
        if (paymentId != null) {
            paymentRepository.markRefunded(paymentId);
        }
        LocalDateTime now = LocalDateTime.now();
        for (OrderCoupon oc : orderCouponRepository.findByOrderNo(orderNo)) {
            userCouponRepository.restoreIfNotExpired(oc.getUserCouponId(), now);
        }
        // 5→6 已退款(终态)
        orderRepository.updateStatusCas(orderNo,
                OrderStatus.AFTER_SALE.code(), OrderStatus.REFUNDED.code());
        eventPublisher.publishEvent(new RefundedEvent(this, orderNo));
    }
}
