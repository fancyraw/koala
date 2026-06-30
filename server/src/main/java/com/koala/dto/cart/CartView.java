package com.koala.dto.cart;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 购物车聚合：行列表 + 勾选有效行合计（失效/售罄行不计入）。 */
@Data
public class CartView {

    private List<CartItemView> items;
    private int totalCount;
    /** 已勾选且有效的件数 */
    private int checkedCount;
    /** 已勾选且有效行的金额合计 */
    private BigDecimal checkedAmount;
}
