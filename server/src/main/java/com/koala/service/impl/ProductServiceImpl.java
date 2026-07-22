package com.koala.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.common.result.PageResult;
import com.koala.dto.product.AdminProductView;
import com.koala.dto.product.ProductCardView;
import com.koala.dto.product.ProductDetailView;
import com.koala.dto.product.ProductSaveRequest;
import com.koala.dto.product.SkuSaveItem;
import com.koala.dto.product.SkuView;
import com.koala.entity.Product;
import com.koala.entity.ProductCategory;
import com.koala.entity.ProductSku;
import com.koala.entity.ProductTag;
import com.koala.enums.ValidFlag;
import com.koala.repository.ProductCategoryRepository;
import com.koala.repository.ProductRepository;
import com.koala.repository.ProductSkuRepository;
import com.koala.repository.ProductTagRepository;
import com.koala.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 低库存预警阈值（后台黄标）：0 < 总库存 < 20。 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final int LOW_STOCK_THRESHOLD = 20;

    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductTagRepository tagRepository;

    public ProductServiceImpl(ProductRepository productRepository, ProductSkuRepository skuRepository,
                              ProductCategoryRepository categoryRepository, ProductTagRepository tagRepository) {
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    public PageResult<ProductCardView> listForUser(Long categoryId, String keyword, long page, long size) {
        IPage<Product> p = productRepository.pageOnSale(categoryId, keyword, page, size);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        return new PageResult<>(toCards(p.getRecords()), p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public List<ProductCardView> hotSelling(int limit) {
        return toCards(productRepository.topOnSale(false, limit));
    }

    @Override
    public List<ProductCardView> recommended(int limit) {
        return toCards(productRepository.topOnSale(true, limit));
    }

    private List<ProductCardView> toCards(List<Product> products) {
        if (products.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<ProductSku>> skuMap = skusByProduct(productIds(products));
        Map<Long, String> tagNames = tagNames(products);
        return products.stream().map(prod -> {
            List<ProductSku> skus = skuMap.getOrDefault(prod.getId(), Collections.emptyList());
            ProductCardView v = new ProductCardView();
            v.setId(prod.getId());
            v.setName(prod.getName());
            v.setMainImage(prod.getMainImage());
            v.setTagName(tagNames.get(prod.getTagId()));
            v.setRecommended(ValidFlag.isEnabled(prod.getIsRecommended()));
            v.setHighlight(firstHighlight(prod.getHighlights()));
            v.setMinPrice(minPrice(skus));
            v.setSalesCount(prod.getSalesCount());
            v.setSoldOut(isSoldOut(skus));
            return v;
        }).collect(Collectors.toList());
    }

    @Override
    public ProductDetailView detailForUser(Long id) {
        Product prod = productRepository.findById(id);
        if (prod == null || !ValidFlag.isEnabled(prod.getIsValid())) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        List<ProductSku> skus = skuRepository.findByProduct(id);

        ProductDetailView v = new ProductDetailView();
        v.setId(prod.getId());
        v.setName(prod.getName());
        v.setMainImage(prod.getMainImage());
        v.setDetailImages(parseArray(prod.getDetailImages()));
        v.setCategoryId(prod.getCategoryId());
        v.setCategoryName(categoryName(prod.getCategoryId()));
        v.setTagId(prod.getTagId());
        v.setTagName(tagName(prod.getTagId()));
        v.setRecommended(ValidFlag.isEnabled(prod.getIsRecommended()));
        v.setHighlights(parseArray(prod.getHighlights()));
        v.setPerOrderLimit(prod.getPerOrderLimit());
        v.setSalesCount(prod.getSalesCount());
        v.setSoldOut(isSoldOut(skus));
        v.setSkus(skus.stream().map(SkuView::of).collect(Collectors.toList()));
        return v;
    }

    @Override
    public PageResult<AdminProductView> listForAdmin(Long categoryId, String keyword, Integer status, long page, long size) {
        IPage<Product> p = productRepository.pageForAdmin(categoryId, keyword, status, page, size);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }

        Map<Long, List<ProductSku>> skuMap = skusByProduct(productIds(p.getRecords()));
        Map<Long, String> tagNames = tagNames(p.getRecords());
        Map<Long, String> catNames = categoryNames(p.getRecords());

        List<AdminProductView> list = p.getRecords().stream().map(prod -> {
            List<ProductSku> skus = skuMap.getOrDefault(prod.getId(), Collections.emptyList());
            return toAdminView(prod, skus, tagNames.get(prod.getTagId()), catNames.get(prod.getCategoryId()));
        }).collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public AdminProductView detailForAdmin(Long id) {
        Product prod = productRepository.findById(id);
        if (prod == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        List<ProductSku> skus = skuRepository.findByProduct(id);
        return toAdminView(prod, skus, tagName(prod.getTagId()), categoryName(prod.getCategoryId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(ProductSaveRequest req, Long adminId) {
        if (categoryRepository.findById(req.getCategoryId()) == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "分类不存在");
        }
        long tagId = req.getTagId() != null ? req.getTagId() : 0L;
        if (tagId != 0L && tagRepository.findById(tagId) == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "标签不存在");
        }

        Product entity = req.getId() != null ? requireProduct(req.getId()) : new Product();
        entity.setName(req.getName());
        entity.setMainImage(req.getMainImage());
        entity.setDetailImages(toJson(req.getDetailImages()));
        entity.setTagId(tagId);
        entity.setIsRecommended(ValidFlag.of(Boolean.TRUE.equals(req.getRecommended())));
        entity.setHighlights(toJson(req.getHighlights()));
        entity.setCategoryId(req.getCategoryId());
        entity.setPerOrderLimit(req.getPerOrderLimit() != null ? req.getPerOrderLimit() : 0);
        entity.setUpdatedBy(adminId);
        if (entity.getId() == null) {
            entity.setCreatedBy(adminId);
            entity.setIsValid(ValidFlag.ENABLED.code());
            productRepository.insert(entity);
        } else {
            productRepository.updateById(entity);
        }
        replaceSkus(entity.getId(), req.getSkus());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireProduct(id);
        skuRepository.deleteByProduct(id);
        productRepository.deleteById(id);
    }

    @Override
    public void setStatus(Long id, boolean valid) {
        requireProduct(id);
        Product patch = new Product();
        patch.setId(id);
        patch.setIsValid(ValidFlag.of(valid));
        productRepository.updateById(patch);
    }

    // ---- SKU 整体替换：传入带 id 的更新、无 id 的新增、库中多余的删除 ----
    private void replaceSkus(Long productId, List<SkuSaveItem> items) {
        List<ProductSku> existing = skuRepository.findByProduct(productId);
        Set<Long> keepIds = items.stream().map(SkuSaveItem::getId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        for (ProductSku old : existing) {
            if (!keepIds.contains(old.getId())) {
                skuRepository.deleteById(old.getId());
            }
        }
        int order = 0;
        for (SkuSaveItem item : items) {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setName(item.getName());
            sku.setPrice(item.getPrice());
            sku.setStock(item.getStock());
            sku.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : order);
            if (item.getId() != null) {
                sku.setId(item.getId());
                skuRepository.updateById(sku);
            } else {
                skuRepository.insert(sku);
            }
            order++;
        }
    }

    private AdminProductView toAdminView(Product prod, List<ProductSku> skus, String tagName, String catName) {
        AdminProductView v = new AdminProductView();
        v.setId(prod.getId());
        v.setName(prod.getName());
        v.setMainImage(prod.getMainImage());
        v.setDetailImages(parseArray(prod.getDetailImages()));
        v.setCategoryId(prod.getCategoryId());
        v.setCategoryName(catName);
        v.setTagId(prod.getTagId());
        v.setTagName(tagName);
        v.setRecommended(ValidFlag.isEnabled(prod.getIsRecommended()));
        v.setHighlights(parseArray(prod.getHighlights()));
        v.setPerOrderLimit(prod.getPerOrderLimit());
        v.setSalesCount(prod.getSalesCount());
        v.setIsValid(prod.getIsValid());
        v.setMinPrice(minPrice(skus));
        int total = skus.stream().mapToInt(s -> s.getStock() != null ? s.getStock() : 0).sum();
        v.setTotalStock(total);
        v.setSoldOut(total == 0);
        v.setLowStock(total > 0 && total < LOW_STOCK_THRESHOLD);
        v.setSkus(skus.stream().map(SkuView::of).collect(Collectors.toList()));
        return v;
    }

    // ---- helpers ----

    private Product requireProduct(Long id) {
        Product p = productRepository.findById(id);
        if (p == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return p;
    }

    private static List<Long> productIds(List<Product> products) {
        return products.stream().map(Product::getId).collect(Collectors.toList());
    }

    private Map<Long, List<ProductSku>> skusByProduct(List<Long> productIds) {
        return skuRepository.findByProducts(productIds)
                .stream().collect(Collectors.groupingBy(ProductSku::getProductId));
    }

    private Map<Long, String> tagNames(List<Product> products) {
        Set<Long> ids = products.stream().map(Product::getTagId)
                .filter(t -> t != null && t != 0L).collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return tagRepository.findByIds(ids).stream()
                .collect(Collectors.toMap(ProductTag::getId, ProductTag::getName));
    }

    private Map<Long, String> categoryNames(List<Product> products) {
        Set<Long> ids = products.stream().map(Product::getCategoryId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryRepository.findByIds(ids).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
    }

    private String tagName(Long tagId) {
        if (tagId == null || tagId == 0L) {
            return null;
        }
        ProductTag t = tagRepository.findById(tagId);
        return t != null ? t.getName() : null;
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        ProductCategory c = categoryRepository.findById(categoryId);
        return c != null ? c.getName() : null;
    }

    private static boolean isSoldOut(List<ProductSku> skus) {
        return skus.stream().allMatch(s -> s.getStock() == null || s.getStock() == 0);
    }

    private static BigDecimal minPrice(List<ProductSku> skus) {
        return skus.stream().map(ProductSku::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(null);
    }

    private static String firstHighlight(String highlightsJson) {
        List<String> arr = parseArray(highlightsJson);
        return arr.isEmpty() ? null : arr.get(0);
    }

    private static List<String> parseArray(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(json, String.class);
    }

    private static String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return JSONUtil.toJsonStr(list);
    }
}
