package com.koala.service;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.product.SortItem;
import com.koala.dto.product.TagSaveRequest;
import com.koala.dto.product.TagView;
import com.koala.entity.ProductTag;
import com.koala.enums.ValidFlag;
import com.koala.repository.ProductRepository;
import com.koala.repository.ProductTagRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final ProductTagRepository tagRepository;
    private final ProductRepository productRepository;

    public TagService(ProductTagRepository tagRepository, ProductRepository productRepository) {
        this.tagRepository = tagRepository;
        this.productRepository = productRepository;
    }

    public List<TagView> listAll() {
        return tagRepository.findAll()
                .stream().map(TagView::of).collect(Collectors.toList());
    }

    public Long save(TagSaveRequest req) {
        ProductTag dup = tagRepository.findByName(req.getName());
        if (dup != null && !dup.getId().equals(req.getId())) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "标签名已存在");
        }

        ProductTag entity = req.getId() != null ? requireExists(req.getId()) : new ProductTag();
        entity.setName(req.getName());
        if (req.getSortOrder() != null) {
            entity.setSortOrder(req.getSortOrder());
        }
        if (req.getValid() != null) {
            entity.setIsValid(ValidFlag.of(req.getValid()));
        }
        if (entity.getId() == null) {
            tagRepository.insert(entity);
        } else {
            tagRepository.updateById(entity);
        }
        return entity.getId();
    }

    public void delete(Long id) {
        requireExists(id);
        if (productRepository.countByTag(id) > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "该标签仍被商品引用，不可删除");
        }
        tagRepository.deleteById(id);
    }

    public void sort(List<SortItem> items) {
        for (SortItem item : items) {
            ProductTag patch = new ProductTag();
            patch.setId(item.getId());
            patch.setSortOrder(item.getSortOrder());
            tagRepository.updateById(patch);
        }
    }

    private ProductTag requireExists(Long id) {
        ProductTag t = tagRepository.findById(id);
        if (t == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return t;
    }
}
