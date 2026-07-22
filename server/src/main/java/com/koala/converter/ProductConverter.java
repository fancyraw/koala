package com.koala.converter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.koala.dto.product.AdminProductView;
import com.koala.dto.product.ProductCardView;
import com.koala.dto.product.SkuView;
import com.koala.entity.Product;
import com.koala.entity.ProductSku;
import com.koala.enums.ValidFlag;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 商品 entity → 视图，含 sku 派生（最低价 / 售罄）与 highlights/detailImages JSON 解析。 */
public final class ProductConverter {

    /** 后台低库存黄标阈值：0 < 总库存 < 20。 */
    private static final int LOW_STOCK_THRESHOLD = 20;

    private ProductConverter() {}

    /** C 端卡片视图（列表）。tagName 需 caller 预加载。 */
    public static ProductCardView toCard(Product prod, List<ProductSku> skus, String tagName) {
        ProductCardView v = new ProductCardView();
        v.setId(prod.getId());
        v.setName(prod.getName());
        v.setMainImage(prod.getMainImage());
        v.setTagName(tagName);
        v.setRecommended(ValidFlag.isEnabled(prod.getIsRecommended()));
        v.setHighlight(firstHighlight(prod.getHighlights()));
        v.setMinPrice(minPrice(skus));
        v.setSalesCount(prod.getSalesCount());
        v.setSoldOut(isSoldOut(skus));
        return v;
    }

    /** 后台视图（详情/列表）。tagName / catName 需 caller 预加载。 */
    public static AdminProductView toAdminView(Product prod, List<ProductSku> skus, String tagName, String catName) {
        AdminProductView v = new AdminProductView();
        v.setId(prod.getId());
        v.setName(prod.getName());
        v.setMainImage(prod.getMainImage());
        v.setDetailImages(parseArray(prod.getDetailImages()));
        v.setCategoryId(prod.getCategoryId());
        v.setCategoryName(catName);
        v.setTagId(prod.getTagId());
        v.setTagName(tagName);
        v.setRecommended(ValidFlag.isEnabled(prod.getIsRecommended()));
        v.setHighlights(parseArray(prod.getHighlights()));
        v.setPerOrderLimit(prod.getPerOrderLimit());
        v.setSalesCount(prod.getSalesCount());
        v.setIsValid(prod.getIsValid());
        v.setMinPrice(minPrice(skus));
        int total = skus.stream().mapToInt(s -> s.getStock() != null ? s.getStock() : 0).sum();
        v.setTotalStock(total);
        v.setSoldOut(total == 0);
        v.setLowStock(total > 0 && total < LOW_STOCK_THRESHOLD);
        v.setSkus(skus.stream().map(SkuView::of).collect(Collectors.toList()));
        return v;
    }

    // ---- 派生助手（也对外暴露，供 ProductService.detailForUser 复用） ----

    public static boolean isSoldOut(List<ProductSku> skus) {
        return skus.stream().allMatch(s -> s.getStock() == null || s.getStock() == 0);
    }

    public static BigDecimal minPrice(List<ProductSku> skus) {
        return skus.stream().map(ProductSku::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(null);
    }

    public static String firstHighlight(String highlightsJson) {
        List<String> arr = parseArray(highlightsJson);
        return arr.isEmpty() ? null : arr.get(0);
    }

    public static List<String> parseArray(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(json, String.class);
    }
}
