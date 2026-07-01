package com.koala.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** 超管启用/禁用管理员。 */
@Data
public class AdminStatusRequest {

    @NotNull(message = "id不能为空")
    private Long id;

    /** true=启用 false=禁用 */
    @NotNull(message = "状态不能为空")
    private Boolean valid;
}
