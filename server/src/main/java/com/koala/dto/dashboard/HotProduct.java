package com.koala.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;

/** 热销Top5一项：商品 + 销量 + 销售额。 */
@Data
public class HotProduct {

    private Long productId;
    private String productName;
    private long quantity;
    private BigDecimal amount;
}
