package com.koala.service;

import com.koala.common.result.PageResult;
import com.koala.dto.product.AdminProductView;
import com.koala.dto.product.ProductCardView;
import com.koala.dto.product.ProductDetailView;
import com.koala.dto.product.ProductSaveRequest;

public interface ProductService {

    /** C端商品列表：仅上架，可按分类/关键词过滤，分页。 */
    PageResult<ProductCardView> listForUser(Long categoryId, String keyword, long page, long size);

    /** C端商品详情：仅上架，否则视为不存在。 */
    ProductDetailView detailForUser(Long id);

    /** 后台商品列表：可按分类/关键词/状态过滤，分页。 */
    PageResult<AdminProductView> listForAdmin(Long categoryId, String keyword, Integer status, long page, long size);

    /** 后台商品详情。 */
    AdminProductView detailForAdmin(Long id);

    /** 后台新增/编辑（含 SKU 整体替换），返回主键。 */
    Long save(ProductSaveRequest req, Long adminId);

    /** 后台删除（连带 SKU）。 */
    void delete(Long id);

    /** 后台上下架。 */
    void setStatus(Long id, boolean valid);
}
