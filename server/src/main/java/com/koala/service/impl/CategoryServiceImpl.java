package com.koala.service.impl;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.product.CategorySaveRequest;
import com.koala.dto.product.CategoryView;
import com.koala.dto.product.SortItem;
import com.koala.entity.ProductCategory;
import com.koala.enums.ValidFlag;
import com.koala.repository.ProductCategoryRepository;
import com.koala.repository.ProductRepository;
import com.koala.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(ProductCategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<CategoryView> listValid() {
        return categoryRepository.findEnabled()
                .stream().map(CategoryView::of).collect(Collectors.toList());
    }

    @Override
    public List<CategoryView> listAll() {
        return categoryRepository.findAll()
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
            entity.setIsValid(ValidFlag.of(req.getValid()));
        }
        if (entity.getId() == null) {
            categoryRepository.insert(entity);
        } else {
            categoryRepository.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void delete(Long id) {
        requireExists(id);
        if (productRepository.countByCategory(id) > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "该分类下存在商品，不可删除");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public void sort(List<SortItem> items) {
        for (SortItem item : items) {
            ProductCategory patch = new ProductCategory();
            patch.setId(item.getId());
            patch.setSortOrder(item.getSortOrder());
            categoryRepository.updateById(patch);
        }
    }

    private ProductCategory requireExists(Long id) {
        ProductCategory c = categoryRepository.findById(id);
        if (c == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return c;
    }
}
