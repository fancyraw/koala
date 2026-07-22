package com.koala.repository;

import com.koala.entity.ProductCategory;

import java.util.Collection;
import java.util.List;

public interface ProductCategoryRepository {

    ProductCategory findById(Long id);

    /** 按主键批量查询。 */
    List<ProductCategory> findByIds(Collection<Long> ids);

    /** 全部分类,按 sortOrder、id 升序。 */
    List<ProductCategory> findAll();

    /** 启用分类,按 sortOrder、id 升序。 */
    List<ProductCategory> findEnabled();

    void insert(ProductCategory category);

    void updateById(ProductCategory category);

    void deleteById(Long id);
}
