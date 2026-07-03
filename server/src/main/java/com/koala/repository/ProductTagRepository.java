package com.koala.repository;

import com.koala.entity.ProductTag;

import java.util.Collection;
import java.util.List;

public interface ProductTagRepository {

    ProductTag findById(Long id);

    List<ProductTag> findByIds(Collection<Long> ids);

    /** 全部标签,按 sortOrder、id 升序。 */
    List<ProductTag> findAll();

    ProductTag findByName(String name);

    void insert(ProductTag tag);

    void updateById(ProductTag tag);

    void deleteById(Long id);
}
