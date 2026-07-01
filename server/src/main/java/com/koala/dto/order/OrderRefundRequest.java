package com.koala.dto.order;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 后台对已完成订单手动发起退款。 */
@Data
public class OrderRefundRequest {

    @NotBlank(message = "订单号不能为空")
    private String no;

    private String reason;
}
