package com.koala.dto.product;

import lombok.Data;

import java.util.List;

/** C端商品详情：含 SKU 列表、详情图、亮点。 */
@Data
public class ProductDetailView {

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
    private boolean soldOut;
    private List<SkuView> skus;
}
