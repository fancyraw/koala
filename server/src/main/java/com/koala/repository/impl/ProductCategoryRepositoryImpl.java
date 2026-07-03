package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.ProductCategory;
import com.koala.enums.ValidFlag;
import com.koala.mapper.ProductCategoryMapper;
import com.koala.repository.ProductCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductCategoryRepositoryImpl implements ProductCategoryRepository {

    private final ProductCategoryMapper categoryMapper;

    public ProductCategoryRepositoryImpl(ProductCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public ProductCategory findById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<ProductCategory> findAll() {
        return categoryMapper.selectList(Wrappers.<ProductCategory>lambdaQuery()
                .orderByAsc(ProductCategory::getSortOrder)
                .orderByAsc(ProductCategory::getId));
    }

    @Override
    public List<ProductCategory> findEnabled() {
        return categoryMapper.selectList(Wrappers.<ProductCategory>lambdaQuery()
                .eq(ProductCategory::getIsValid, ValidFlag.ENABLED.code())
                .orderByAsc(ProductCategory::getSortOrder)
                .orderByAsc(ProductCategory::getId));
    }

    @Override
    public void insert(ProductCategory category) {
        categoryMapper.insert(category);
    }

    @Override
    public void updateById(ProductCategory category) {
        categoryMapper.updateById(category);
    }

    @Override
    public void deleteById(Long id) {
        categoryMapper.deleteById(id);
    }
}
