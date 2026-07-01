package com.koala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** 退款成功事件 → 按策略回滚销量、业务日志。 */
@Getter
public class RefundedEvent extends ApplicationEvent {

    private final String orderNo;

    public RefundedEvent(Object source, String orderNo) {
        super(source);
        this.orderNo = orderNo;
    }
}
