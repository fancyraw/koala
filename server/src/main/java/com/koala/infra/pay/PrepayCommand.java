package com.koala.infra.pay;

import lombok.Data;

import java.math.BigDecimal;

/** 统一下单入参。 */
@Data
public class PrepayCommand {

    private String orderNo;
    private BigDecimal payAmount;
    private String openid;
    private String description;
}
