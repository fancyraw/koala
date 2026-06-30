package com.koala.common.web;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** 通用 id 请求体（POST 无路径变量约定）。 */
@Data
public class IdRequest {

    @NotNull(message = "id不能为空")
    private Long id;
}
