package com.koala.dto.cart;

import lombok.Data;

import java.math.BigDecimal;

/** 购物车行：价格/库存实时取，失效与售罄为派生态。 */
@Data
public class CartItemView {

    private Long id;
    private Long productId;
    private Long skuId;
    private String productName;
    private String mainImage;
    private String skuName;
    /** 当前售价（实时取自 SKU） */
    private BigDecimal price;
    private Integer quantity;
    private boolean checked;
    /** 当前 SKU 库存 */
    private Integer stock;
    /** 商品级单次限购（0=不限） */
    private Integer perOrderLimit;
    /** 派生：商品下架或 SKU/商品已删除 */
    private boolean invalid;
    /** 派生：上架在售但该 SKU 库存为 0 */
    private boolean soldOut;
}
