package com.koala.repository;

import com.koala.entity.ProductSku;

import java.util.Collection;
import java.util.List;

public interface ProductSkuRepository {

    ProductSku findById(Long id);

    List<ProductSku> findByIds(Collection<Long> ids);

    /** 按商品取 SKU,按 sortOrder、id 升序。 */
    List<ProductSku> findByProduct(Long productId);

    /** 多商品批量取 SKU(排序同上),供列表页组装。 */
    List<ProductSku> findByProducts(Collection<Long> productIds);

    void insert(ProductSku sku);

    void updateById(ProductSku sku);

    void deleteById(Long id);

    void deleteByProduct(Long productId);

    /** 乐观扣减库存:stock>=qty 才成功。返回受影响行数。 */
    int deductStock(Long skuId, int qty);

    /** 回补库存。 */
    void addStock(Long skuId, int qty);
}
