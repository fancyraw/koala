package com.koala.infra.pay;

import lombok.Data;

/** 退款结果。 */
@Data
public class RefundResult {

    private boolean success;
    private String refundId;
}
