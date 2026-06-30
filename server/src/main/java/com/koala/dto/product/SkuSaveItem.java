package com.koala.dto.product;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class SkuSaveItem {

    /** 已有 SKU 带 id，新增为空。 */
    private Long id;

    @NotBlank(message = "规格名不能为空")
    @Size(max = 64, message = "规格名过长")
    private String name;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能为负")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    private Integer sortOrder;
}
