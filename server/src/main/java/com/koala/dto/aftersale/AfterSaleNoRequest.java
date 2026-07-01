package com.koala.dto.aftersale;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 售后单号入 body 的通用请求(撤销/详情写操作)。 */
@Data
public class AfterSaleNoRequest {

    @NotBlank(message = "售后单号不能为空")
    private String afterSaleNo;
}
