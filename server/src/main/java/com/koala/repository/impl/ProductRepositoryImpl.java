package com.koala.repository.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koala.entity.Product;
import com.koala.enums.ValidFlag;
import com.koala.mapper.ProductMapper;
import com.koala.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductMapper productMapper;

    public ProductRepositoryImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public Product findById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public List<Product> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return productMapper.selectBatchIds(ids);
    }

    @Override
    public IPage<Product> pageOnSale(Long categoryId, String keyword, long page, long size) {
        return productMapper.selectPage(new Page<>(page, size),
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getIsValid, ValidFlag.ENABLED.code())
                        .eq(categoryId != null, Product::getCategoryId, categoryId)
                        .like(StrUtil.isNotBlank(keyword), Product::getName, keyword)
                        .orderByDesc(Product::getIsRecommended)
                        .orderByDesc(Product::getSalesCount)
                        .orderByDesc(Product::getId));
    }

    @Override
    public IPage<Product> pageForAdmin(Long categoryId, String keyword, Integer isValid, long page, long size) {
        return productMapper.selectPage(new Page<>(page, size),
                Wrappers.<Product>lambdaQuery()
                        .eq(categoryId != null, Product::getCategoryId, categoryId)
                        .eq(isValid != null, Product::getIsValid, isValid)
                        .like(StrUtil.isNotBlank(keyword), Product::getName, keyword)
                        .orderByDesc(Product::getId));
    }

    @Override
    public List<Product> topOnSale(boolean recommendedOnly, int limit) {
        return productMapper.selectList(Wrappers.<Product>lambdaQuery()
                .eq(Product::getIsValid, ValidFlag.ENABLED.code())
                .eq(recommendedOnly, Product::getIsRecommended, ValidFlag.ENABLED.code())
                .orderByDesc(Product::getSalesCount)
                .orderByDesc(Product::getId)
                .last("LIMIT " + limit));
    }

    @Override
    public long countByCategory(Long categoryId) {
        Long c = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getCategoryId, categoryId));
        return c == null ? 0 : c;
    }

    @Override
    public long countByTag(Long tagId) {
        Long c = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getTagId, tagId));
        return c == null ? 0 : c;
    }

    @Override
    public void insert(Product product) {
        productMapper.insert(product);
    }

    @Override
    public void updateById(Product product) {
        productMapper.updateById(product);
    }

    @Override
    public void deleteById(Long id) {
        productMapper.deleteById(id);
    }

    @Override
    public void addSalesCount(Long productId, int delta) {
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .setSql("sales_count = GREATEST(0, sales_count + (" + delta + "))")
                .eq(Product::getId, productId));
    }
}
