package com.koala.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.cart.CartAddRequest;
import com.koala.dto.cart.CartItemView;
import com.koala.dto.cart.CartUpdateRequest;
import com.koala.dto.cart.CartView;
import com.koala.entity.CartItem;
import com.koala.entity.Product;
import com.koala.entity.ProductSku;
import com.koala.mapper.CartItemMapper;
import com.koala.mapper.ProductMapper;
import com.koala.mapper.ProductSkuMapper;
import com.koala.service.CartService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;

    public CartServiceImpl(CartItemMapper cartMapper, ProductSkuMapper skuMapper, ProductMapper productMapper) {
        this.cartMapper = cartMapper;
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
    }

    @Override
    public CartView list(Long userId) {
        List<CartItem> items = cartMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        if (items.isEmpty()) {
            return emptyView();
        }

        Set<Long> skuIds = items.stream().map(CartItem::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuMap = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        Set<Long> productIds = skuMap.values().stream().map(ProductSku::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));

        CartView view = new CartView();
        view.setItems(items.stream().map(it -> toView(it, skuMap, productMap)).collect(Collectors.toList()));
        view.setTotalCount(view.getItems().stream().mapToInt(CartItemView::getQuantity).sum());

        BigDecimal amount = BigDecimal.ZERO;
        int checkedCount = 0;
        for (CartItemView v : view.getItems()) {
            if (v.isChecked() && !v.isInvalid() && !v.isSoldOut()) {
                amount = amount.add(v.getPrice().multiply(BigDecimal.valueOf(v.getQuantity())));
                checkedCount += v.getQuantity();
            }
        }
        view.setCheckedCount(checkedCount);
        view.setCheckedAmount(amount);
        return view;
    }

    @Override
    public CartView add(Long userId, CartAddRequest req) {
        ProductSku sku = skuMapper.selectById(req.getSkuId());
        if (sku == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null || product.getIsValid() == null || product.getIsValid() != 1) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "商品已下架");
        }

        CartItem existing = cartMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, req.getSkuId()));
        int targetQty = (existing != null ? existing.getQuantity() : 0) + req.getQuantity();
        checkStockAndLimit(sku, product, targetQty);

        if (existing != null) {
            existing.setQuantity(targetQty);
            existing.setChecked(1);
            cartMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(product.getId());
            item.setSkuId(req.getSkuId());
            item.setQuantity(req.getQuantity());
            item.setChecked(1);
            try {
                cartMapper.insert(item);
            } catch (DuplicateKeyException e) {
                // 并发下同 SKU 已插入，转为累加
                CartItem now = cartMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getSkuId, req.getSkuId()));
                if (now != null) {
                    int merged = now.getQuantity() + req.getQuantity();
                    checkStockAndLimit(sku, product, merged);
                    now.setQuantity(merged);
                    now.setChecked(1);
                    cartMapper.updateById(now);
                }
            }
        }
        return list(userId);
    }

    @Override
    public CartView update(Long userId, CartUpdateRequest req) {
        CartItem item = requireOwned(userId, req.getId());
        if (req.getQuantity() != null) {
            ProductSku sku = skuMapper.selectById(item.getSkuId());
            Product product = sku != null ? productMapper.selectById(sku.getProductId()) : null;
            if (sku == null || product == null) {
                throw new BizException(ErrorCode.DATA_NOT_FOUND);
            }
            checkStockAndLimit(sku, product, req.getQuantity());
            item.setQuantity(req.getQuantity());
        }
        if (req.getChecked() != null) {
            item.setChecked(req.getChecked() ? 1 : 0);
        }
        cartMapper.updateById(item);
        return list(userId);
    }

    @Override
    public CartView remove(Long userId, List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            cartMapper.delete(Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getUserId, userId)
                    .in(CartItem::getId, ids));
        }
        return list(userId);
    }

    private void checkStockAndLimit(ProductSku sku, Product product, int targetQty) {
        int stock = sku.getStock() != null ? sku.getStock() : 0;
        if (targetQty > stock) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        int limit = product.getPerOrderLimit() != null ? product.getPerOrderLimit() : 0;
        if (limit > 0 && targetQty > limit) {
            throw new BizException(ErrorCode.PURCHASE_LIMIT.getCode(),
                    "该商品每单限购 " + limit + " 件");
        }
    }

    private CartItem requireOwned(Long userId, Long id) {
        CartItem item = cartMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return item;
    }

    private CartItemView toView(CartItem item, Map<Long, ProductSku> skuMap, Map<Long, Product> productMap) {
        ProductSku sku = skuMap.get(item.getSkuId());
        Product product = sku != null ? productMap.get(sku.getProductId()) : null;

        CartItemView v = new CartItemView();
        v.setId(item.getId());
        v.setProductId(item.getProductId());
        v.setSkuId(item.getSkuId());
        v.setQuantity(item.getQuantity());
        v.setChecked(item.getChecked() != null && item.getChecked() == 1);

        boolean invalid = sku == null || product == null
                || product.getIsValid() == null || product.getIsValid() != 1;
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

    private CartView emptyView() {
        CartView view = new CartView();
        view.setItems(Collections.emptyList());
        view.setTotalCount(0);
        view.setCheckedCount(0);
        view.setCheckedAmount(BigDecimal.ZERO);
        return view;
    }
}
