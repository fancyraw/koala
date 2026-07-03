package com.koala.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koala.entity.Product;

import java.util.Collection;
import java.util.List;

public interface ProductRepository {

    Product findById(Long id);

    List<Product> findByIds(Collection<Long> ids);

    /** C 端上架分页:可选分类/关键字,按推荐、销量、id 倒序。 */
    IPage<Product> pageOnSale(Long categoryId, String keyword, long page, long size);

    /** 后台分页:可选分类/上架状态/关键字,按 id 倒序。 */
    IPage<Product> pageForAdmin(Long categoryId, String keyword, Integer isValid, long page, long size);

    /** 上架商品按销量倒序取前 limit;recommendedOnly=true 仅推荐位。 */
    List<Product> topOnSale(boolean recommendedOnly, int limit);

    long countByCategory(Long categoryId);

    long countByTag(Long tagId);

    void insert(Product product);

    void updateById(Product product);

    void deleteById(Long id);

    /** 按商品累加销量(下限 0),用于支付/退款副作用。 */
    void addSalesCount(Long productId, int delta);
}
