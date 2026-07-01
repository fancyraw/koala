package com.koala.infra.pay;

import lombok.Data;

import java.math.BigDecimal;

/** 退款入参。 */
@Data
public class RefundCommand {

    private String orderNo;
    private String transactionId;
    private BigDecimal refundAmount;
    private BigDecimal totalAmount;
    private String reason;
}
