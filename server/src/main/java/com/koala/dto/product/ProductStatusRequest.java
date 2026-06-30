package com.koala.dto.product;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ProductStatusRequest {

    @NotNull(message = "id不能为空")
    private Long id;

    /** true=上架 false=下架 */
    @NotNull(message = "状态不能为空")
    private Boolean valid;
}
