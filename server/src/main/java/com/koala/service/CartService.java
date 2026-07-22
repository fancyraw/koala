package com.koala.service;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.converter.CartConverter;
import com.koala.dto.cart.CartAddRequest;
import com.koala.dto.cart.CartItemView;
import com.koala.dto.cart.CartUpdateRequest;
import com.koala.dto.cart.CartView;
import com.koala.entity.CartItem;
import com.koala.entity.Product;
import com.koala.entity.ProductSku;
import com.koala.enums.ValidFlag;
import com.koala.repository.CartItemRepository;
import com.koala.repository.ProductRepository;
import com.koala.repository.ProductSkuRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartItemRepository cartRepository;
    private final ProductSkuRepository skuRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartRepository, ProductSkuRepository skuRepository,
                           ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
    }

    public CartView list(Long userId) {
        List<CartItem> items = cartRepository.findByUser(userId);
        if (items.isEmpty()) {
            return CartConverter.emptyView();
        }

        Set<Long> skuIds = items.stream().map(CartItem::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuMap = skuRepository.findByIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        Set<Long> productIds = skuMap.values().stream().map(ProductSku::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productRepository.findByIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));

        CartView view = new CartView();
        view.setItems(items.stream().map(it -> CartConverter.toView(it, skuMap, productMap)).collect(Collectors.toList()));
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

    public CartView add(Long userId, CartAddRequest req) {
        ProductSku sku = skuRepository.findById(req.getSkuId());
        if (sku == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        Product product = productRepository.findById(sku.getProductId());
        if (product == null || !ValidFlag.isEnabled(product.getIsValid())) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "商品已下架");
        }

        CartItem existing = cartRepository.findByUserAndSku(userId, req.getSkuId());
        int targetQty = (existing != null ? existing.getQuantity() : 0) + req.getQuantity();
        checkStockAndLimit(sku, product, targetQty);

        if (existing != null) {
            existing.setQuantity(targetQty);
            existing.setChecked(ValidFlag.ENABLED.code());
            cartRepository.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(product.getId());
            item.setSkuId(req.getSkuId());
            item.setQuantity(req.getQuantity());
            item.setChecked(ValidFlag.ENABLED.code());
            try {
                cartRepository.insert(item);
            } catch (DuplicateKeyException e) {
                // 并发下同 SKU 已插入，转为累加
                CartItem now = cartRepository.findByUserAndSku(userId, req.getSkuId());
                if (now != null) {
                    int merged = now.getQuantity() + req.getQuantity();
                    checkStockAndLimit(sku, product, merged);
                    now.setQuantity(merged);
                    now.setChecked(ValidFlag.ENABLED.code());
                    cartRepository.updateById(now);
                }
            }
        }
        return list(userId);
    }

    public CartView update(Long userId, CartUpdateRequest req) {
        CartItem item = requireOwned(userId, req.getId());
        if (req.getQuantity() != null) {
            ProductSku sku = skuRepository.findById(item.getSkuId());
            Product product = sku != null ? productRepository.findById(sku.getProductId()) : null;
            if (sku == null || product == null) {
                throw new BizException(ErrorCode.DATA_NOT_FOUND);
            }
            checkStockAndLimit(sku, product, req.getQuantity());
            item.setQuantity(req.getQuantity());
        }
        if (req.getChecked() != null) {
            item.setChecked(ValidFlag.of(req.getChecked()));
        }
        cartRepository.updateById(item);
        return list(userId);
    }

    public CartView remove(Long userId, List<Long> ids) {
        cartRepository.deleteByUserAndIds(userId, ids);
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
        CartItem item = cartRepository.findById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return item;
    }

}
