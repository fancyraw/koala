package com.koala.dto.product;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class SortItem {

    @NotNull(message = "id不能为空")
    private Long id;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;
}
