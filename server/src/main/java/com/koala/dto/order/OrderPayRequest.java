package com.koala.dto.order;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 取支付参数。 */
@Data
public class OrderPayRequest {

    @NotBlank(message = "订单号不能为空")
    private String no;
}
