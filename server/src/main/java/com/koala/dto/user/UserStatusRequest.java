package com.koala.dto.user;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UserStatusRequest {

    @NotNull(message = "id不能为空")
    private Long id;

    /** true=启用 false=禁用 */
    @NotNull(message = "状态不能为空")
    private Boolean valid;
}
