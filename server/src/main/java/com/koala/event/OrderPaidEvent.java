package com.koala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** 订单支付成功事件 → 销量+1、业务日志(预留推送)。 */
@Getter
public class OrderPaidEvent extends ApplicationEvent {

    private final String orderNo;

    public OrderPaidEvent(Object source, String orderNo) {
        super(source);
        this.orderNo = orderNo;
    }
}
