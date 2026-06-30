package com.koala.common.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 通用分页入参：?page=1&size=20。
 */
@Data
public class PageQuery {

    @Min(value = 1, message = "页码从 1 开始")
    private long page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private long size = 20;
}
