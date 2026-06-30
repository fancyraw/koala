package com.koala.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 后台商品列表/详情：库存为全 SKU 之和，含派生预警/售罄。 */
@Data
public class AdminProductView {

    private Long id;
    private String name;
    private String mainImage;
    private List<String> detailImages;
    private Long categoryId;
    private String categoryName;
    private Long tagId;
    private String tagName;
    private boolean recommended;
    private List<String> highlights;
    private Integer perOrderLimit;
    private Integer salesCount;
    /** 1=上架 0=下架 */
    private Integer isValid;
    private BigDecimal minPrice;
    /** 全 SKU 库存之和 */
    private Integer totalStock;
    /** 派生:全 SKU 库存均为 0 */
    private boolean soldOut;
    /** 派生:0<总库存<20 */
    private boolean lowStock;
    private List<SkuView> skus;
}
