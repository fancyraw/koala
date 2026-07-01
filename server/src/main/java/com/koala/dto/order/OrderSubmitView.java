package com.koala.dto.order;

import lombok.Data;

import java.math.BigDecimal;

/** 提交订单结果：订单号 + 实付(供前端调起支付)。 */
@Data
public class OrderSubmitView {

    private String orderNo;
    private BigDecimal payAmount;
}
