package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.ProductTag;
import com.koala.mapper.ProductTagMapper;
import com.koala.repository.ProductTagRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class ProductTagRepositoryImpl implements ProductTagRepository {

    private final ProductTagMapper tagMapper;

    public ProductTagRepositoryImpl(ProductTagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Override
    public ProductTag findById(Long id) {
        return tagMapper.selectById(id);
    }

    @Override
    public List<ProductTag> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(ids);
    }

    @Override
    public List<ProductTag> findAll() {
        return tagMapper.selectList(Wrappers.<ProductTag>lambdaQuery()
                .orderByAsc(ProductTag::getSortOrder)
                .orderByAsc(ProductTag::getId));
    }

    @Override
    public ProductTag findByName(String name) {
        return tagMapper.selectOne(Wrappers.<ProductTag>lambdaQuery()
                .eq(ProductTag::getName, name)
                .last("LIMIT 1"));
    }

    @Override
    public void insert(ProductTag tag) {
        tagMapper.insert(tag);
    }

    @Override
    public void updateById(ProductTag tag) {
        tagMapper.updateById(tag);
    }

    @Override
    public void deleteById(Long id) {
        tagMapper.deleteById(id);
    }
}
