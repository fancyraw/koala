package com.koala.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.product.CategorySaveRequest;
import com.koala.dto.product.CategoryView;
import com.koala.dto.product.SortItem;
import com.koala.entity.Product;
import com.koala.entity.ProductCategory;
import com.koala.mapper.ProductCategoryMapper;
import com.koala.mapper.ProductMapper;
import com.koala.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    public CategoryServiceImpl(ProductCategoryMapper categoryMapper, ProductMapper productMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<CategoryView> listValid() {
        return categoryMapper.selectList(Wrappers.<ProductCategory>lambdaQuery()
                        .eq(ProductCategory::getIsValid, 1)
                        .orderByAsc(ProductCategory::getSortOrder)
                        .orderByAsc(ProductCategory::getId))
                .stream().map(CategoryView::of).collect(Collectors.toList());
    }

    @Override
    public List<CategoryView> listAll() {
        return categoryMapper.selectList(Wrappers.<ProductCategory>lambdaQuery()
                        .orderByAsc(ProductCategory::getSortOrder)
                        .orderByAsc(ProductCategory::getId))
                .stream().map(CategoryView::of).collect(Collectors.toList());
    }

    @Override
    public Long save(CategorySaveRequest req) {
        ProductCategory entity = req.getId() != null ? requireExists(req.getId()) : new ProductCategory();
        entity.setName(req.getName());
        entity.setIconUrl(req.getIconUrl() != null ? req.getIconUrl() : "");
        if (req.getSortOrder() != null) {
            entity.setSortOrder(req.getSortOrder());
        }
        if (req.getValid() != null) {
            entity.setIsValid(req.getValid() ? 1 : 0);
        }
        if (entity.getId() == null) {
            categoryMapper.insert(entity);
        } else {
            categoryMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void delete(Long id) {
        requireExists(id);
        long count = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getCategoryId, id));
        if (count > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "该分类下存在商品，不可删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public void sort(List<SortItem> items) {
        for (SortItem item : items) {
            ProductCategory patch = new ProductCategory();
            patch.setId(item.getId());
            patch.setSortOrder(item.getSortOrder());
            categoryMapper.updateById(patch);
        }
    }

    private ProductCategory requireExists(Long id) {
        ProductCategory c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return c;
    }
}
