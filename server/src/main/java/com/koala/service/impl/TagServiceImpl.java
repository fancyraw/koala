package com.koala.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.product.SortItem;
import com.koala.dto.product.TagSaveRequest;
import com.koala.dto.product.TagView;
import com.koala.entity.Product;
import com.koala.entity.ProductTag;
import com.koala.mapper.ProductMapper;
import com.koala.mapper.ProductTagMapper;
import com.koala.service.TagService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    private final ProductTagMapper tagMapper;
    private final ProductMapper productMapper;

    public TagServiceImpl(ProductTagMapper tagMapper, ProductMapper productMapper) {
        this.tagMapper = tagMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<TagView> listAll() {
        return tagMapper.selectList(Wrappers.<ProductTag>lambdaQuery()
                        .orderByAsc(ProductTag::getSortOrder)
                        .orderByAsc(ProductTag::getId))
                .stream().map(TagView::of).collect(Collectors.toList());
    }

    @Override
    public Long save(TagSaveRequest req) {
        ProductTag dup = tagMapper.selectOne(Wrappers.<ProductTag>lambdaQuery()
                .eq(ProductTag::getName, req.getName())
                .last("LIMIT 1"));
        if (dup != null && !dup.getId().equals(req.getId())) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "标签名已存在");
        }

        ProductTag entity = req.getId() != null ? requireExists(req.getId()) : new ProductTag();
        entity.setName(req.getName());
        if (req.getSortOrder() != null) {
            entity.setSortOrder(req.getSortOrder());
        }
        if (req.getValid() != null) {
            entity.setIsValid(req.getValid() ? 1 : 0);
        }
        if (entity.getId() == null) {
            tagMapper.insert(entity);
        } else {
            tagMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void delete(Long id) {
        requireExists(id);
        long count = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getTagId, id));
        if (count > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "该标签仍被商品引用，不可删除");
        }
        tagMapper.deleteById(id);
    }

    @Override
    public void sort(List<SortItem> items) {
        for (SortItem item : items) {
            ProductTag patch = new ProductTag();
            patch.setId(item.getId());
            patch.setSortOrder(item.getSortOrder());
            tagMapper.updateById(patch);
        }
    }

    private ProductTag requireExists(Long id) {
        ProductTag t = tagMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return t;
    }
}
