package com.koala.dto.product;

import lombok.Data;

import java.math.BigDecimal;

/** C端列表卡片：最低 SKU 价为展示价，售罄为派生态。 */
@Data
public class ProductCardView {

    private Long id;
    private String name;
    private String mainImage;
    private String tagName;
    private boolean recommended;
    /** 精选推荐卡复用首条亮点作种草语 */
    private String highlight;
    private BigDecimal minPrice;
    private Integer salesCount;
    private boolean soldOut;
}
