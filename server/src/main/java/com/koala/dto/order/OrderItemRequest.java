package com.koala.dto.order;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/** 下单商品项：规格+数量。 */
@Data
public class OrderItemRequest {

    @NotNull(message = "规格不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
}
