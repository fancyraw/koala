package com.koala.dto.cart;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/** 更新购物车行：数量与勾选状态可分别更新（任一非空才改）。 */
@Data
public class CartUpdateRequest {

    @NotNull(message = "id不能为空")
    private Long id;

    /** 目标数量（绝对值，非累加）；为空表示不改数量。 */
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    /** 勾选状态；为空表示不改勾选。 */
    private Boolean checked;
}
