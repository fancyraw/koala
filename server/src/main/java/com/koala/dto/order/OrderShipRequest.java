package com.koala.dto.order;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 后台发货：订单号 + 物流公司 + 物流单号。 */
@Data
public class OrderShipRequest {

    @NotBlank(message = "订单号不能为空")
    private String no;

    @NotBlank(message = "物流公司不能为空")
    private String logisticsCompany;

    @NotBlank(message = "物流单号不能为空")
    private String logisticsNo;
}
