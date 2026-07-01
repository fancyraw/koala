package com.koala.dto.order;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 订单号入 body 的通用请求(取消/确认收货/删除)。 */
@Data
public class OrderNoRequest {

    @NotBlank(message = "订单号不能为空")
    private String no;
}
