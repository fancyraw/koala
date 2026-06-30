package com.koala.dto.cart;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class CartAddRequest {

    @NotNull(message = "规格不能为空")
    private Long skuId;

    /** 加购数量（与已有数量累加），默认1。 */
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity = 1;
}
