package com.koala.task;

import com.koala.service.CouponService;
import com.koala.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 业务定时任务(7.4)：订单超时取消、自动确认收货、优惠券过期。
 * 单机单副本部署，不引入 ShedLock；扩多副本时在任务入口加 Redisson 锁即可。
 */
@Slf4j
@Component
public class OrderScheduler {

    private final OrderService orderService;
    private final CouponService couponService;

    public OrderScheduler(OrderService orderService, CouponService couponService) {
        this.orderService = orderService;
        this.couponService = couponService;
    }

    /** 订单超时取消：每分钟。status=0 且 expire_at<now → 释放库存/券 → 已取消。 */
    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        try {
            int n = orderService.autoCancelTimeout();
            if (n > 0) {
                log.info("超时取消订单 {} 单", n);
            }
        } catch (Exception e) {
            log.error("订单超时取消任务异常", e);
        }
    }

    /** 自动确认收货：每天凌晨 0:10。status=2 且发货超 auto_confirm_days → 已完成。 */
    @Scheduled(cron = "0 10 0 * * ?")
    public void autoConfirmOrders() {
        try {
            int n = orderService.autoConfirmReceived();
            if (n > 0) {
                log.info("自动确认收货 {} 单", n);
            }
        } catch (Exception e) {
            log.error("自动确认收货任务异常", e);
        }
    }

    /** 优惠券过期：每天凌晨 0:05。未使用且已到期 → 已过期。 */
    @Scheduled(cron = "0 5 0 * * ?")
    public void expireCoupons() {
        try {
            int n = couponService.expireOverdue();
            if (n > 0) {
                log.info("过期优惠券 {} 张", n);
            }
        } catch (Exception e) {
            log.error("优惠券过期任务异常", e);
        }
    }
}
