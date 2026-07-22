package com.koala.converter;

import com.koala.dto.cart.CartItemView;
import com.koala.dto.cart.CartView;
import com.koala.entity.CartItem;
import com.koala.entity.Product;
import com.koala.entity.ProductSku;
import com.koala.enums.ValidFlag;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/** 购物车 entity → 视图。skuMap/productMap 由调用方预加载后传入。 */
public final class CartConverter {

    private CartConverter() {}

    public static CartItemView toView(CartItem item, Map<Long, ProductSku> skuMap, Map<Long, Product> productMap) {
        ProductSku sku = skuMap.get(item.getSkuId());
        Product product = sku != null ? productMap.get(sku.getProductId()) : null;

        CartItemView v = new CartItemView();
        v.setId(item.getId());
        v.setProductId(item.getProductId());
        v.setSkuId(item.getSkuId());
        v.setQuantity(item.getQuantity());
        v.setChecked(ValidFlag.isEnabled(item.getChecked()));

        boolean invalid = sku == null || product == null
                || !ValidFlag.isEnabled(product.getIsValid());
        v.setInvalid(invalid);
        if (sku != null) {
            v.setSkuName(sku.getName());
            v.setPrice(sku.getPrice());
            v.setStock(sku.getStock());
        } else {
            v.setPrice(BigDecimal.ZERO);
            v.setStock(0);
        }
        if (product != null) {
            v.setProductName(product.getName());
            v.setMainImage(product.getMainImage());
            v.setPerOrderLimit(product.getPerOrderLimit());
        }
        v.setSoldOut(!invalid && (sku.getStock() == null || sku.getStock() == 0));
        return v;
    }

    public static CartView emptyView() {
        CartView view = new CartView();
        view.setItems(Collections.emptyList());
        view.setTotalCount(0);
        view.setCheckedCount(0);
        view.setCheckedAmount(BigDecimal.ZERO);
        return view;
    }
}
