package com.koala.dto.aftersale;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 后台审核售后单：同意/拒绝。拒绝须填理由。 */
@Data
public class AfterSaleAuditRequest {

    @NotBlank(message = "售后单号不能为空")
    private String afterSaleNo;

    /** true=同意 false=拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    private String auditRemark;
}
