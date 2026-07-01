package com.koala.infra.pay;

import javax.servlet.http.HttpServletRequest;

/** 支付渠道抽象(扩展点)：新增渠道只需实现并注册，PaymentChannelFactory 按 code 路由。 */
public interface PaymentChannel {

    String code();

    PrepayResult prepay(PrepayCommand cmd);

    NotifyResult parseNotify(HttpServletRequest request);

    RefundResult refund(RefundCommand cmd);
}
