package com.koala.dto.product;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class ProductSaveRequest {

    /** 新增为空，编辑必填。 */
    private Long id;

    @NotBlank(message = "商品名不能为空")
    @Size(max = 128, message = "商品名过长")
    private String name;

    @NotBlank(message = "主图不能为空")
    private String mainImage;

    /** 详情图URL数组。 */
    private List<String> detailImages;

    /** 标签ID，可空（0/null=无）。 */
    private Long tagId;

    private Boolean recommended;

    /** 产品亮点数组。 */
    private List<String> highlights;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Min(value = 0, message = "限购数量不能为负")
    private Integer perOrderLimit;

    @NotEmpty(message = "至少一个规格")
    @Size(max = 4, message = "最多4个规格")
    @Valid
    private List<SkuSaveItem> skus;
}
