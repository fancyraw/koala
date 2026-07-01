package com.koala.infra.pay;

import lombok.Data;

import java.math.BigDecimal;

/** 回调解析+验签结果。 */
@Data
public class NotifyResult {

    private boolean success;
    private String orderNo;
    private String transactionId;
    private BigDecimal payAmount;
}
