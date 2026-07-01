package com.koala.dto.aftersale;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 买家填写/修改寄回单号(退货退款,待寄回或待商家收货阶段可填)。 */
@Data
public class AfterSaleTrackingRequest {

    @NotBlank(message = "售后单号不能为空")
    private String afterSaleNo;

    @NotBlank(message = "寄回单号不能为空")
    private String returnTrackingNo;
}
