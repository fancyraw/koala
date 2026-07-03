package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.ProductSku;
import com.koala.mapper.ProductSkuMapper;
import com.koala.repository.ProductSkuRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class ProductSkuRepositoryImpl implements ProductSkuRepository {

    private final ProductSkuMapper skuMapper;

    public ProductSkuRepositoryImpl(ProductSkuMapper skuMapper) {
        this.skuMapper = skuMapper;
    }

    @Override
    public ProductSku findById(Long id) {
        return skuMapper.selectById(id);
    }

    @Override
    public List<ProductSku> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return skuMapper.selectBatchIds(ids);
    }

    @Override
    public List<ProductSku> findByProduct(Long productId) {
        return skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .eq(ProductSku::getProductId, productId)
                .orderByAsc(ProductSku::getSortOrder)
                .orderByAsc(ProductSku::getId));
    }

    @Override
    public List<ProductSku> findByProducts(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return skuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                .in(ProductSku::getProductId, productIds)
                .orderByAsc(ProductSku::getSortOrder)
                .orderByAsc(ProductSku::getId));
    }

    @Override
    public void insert(ProductSku sku) {
        skuMapper.insert(sku);
    }

    @Override
    public void updateById(ProductSku sku) {
        skuMapper.updateById(sku);
    }

    @Override
    public void deleteById(Long id) {
        skuMapper.deleteById(id);
    }

    @Override
    public void deleteByProduct(Long productId) {
        skuMapper.delete(Wrappers.<ProductSku>lambdaQuery()
                .eq(ProductSku::getProductId, productId));
    }

    @Override
    public int deductStock(Long skuId, int qty) {
        return skuMapper.update(null, Wrappers.<ProductSku>lambdaUpdate()
                .setSql("stock = stock - " + qty)
                .eq(ProductSku::getId, skuId)
                .ge(ProductSku::getStock, qty));
    }

    @Override
    public void addStock(Long skuId, int qty) {
        skuMapper.update(null, Wrappers.<ProductSku>lambdaUpdate()
                .setSql("stock = stock + " + qty)
                .eq(ProductSku::getId, skuId));
    }
}
