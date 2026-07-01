package com.koala.dto.order;

import lombok.Data;

import java.math.BigDecimal;

/** 订单商品行(预览用实时数据 / 详情用快照)。 */
@Data
public class OrderItemView {

    private Long productId;
    private Long skuId;
    private String productName;
    private String skuName;
    private String productImage;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
