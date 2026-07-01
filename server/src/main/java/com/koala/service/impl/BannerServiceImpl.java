package com.koala.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.content.BannerSaveRequest;
import com.koala.dto.content.BannerView;
import com.koala.dto.product.SortItem;
import com.koala.entity.Banner;
import com.koala.mapper.BannerMapper;
import com.koala.service.BannerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;

    public BannerServiceImpl(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    @Override
    public List<BannerView> listValid() {
        return bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                        .eq(Banner::getIsValid, 1)
                        .orderByAsc(Banner::getSortOrder)
                        .orderByAsc(Banner::getId))
                .stream().map(BannerView::of).collect(Collectors.toList());
    }

    @Override
    public List<BannerView> listAll() {
        return bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                        .orderByAsc(Banner::getSortOrder)
                        .orderByAsc(Banner::getId))
                .stream().map(BannerView::of).collect(Collectors.toList());
    }

    @Override
    public Long save(BannerSaveRequest req) {
        Banner entity = req.getId() != null ? requireExists(req.getId()) : new Banner();
        entity.setImageUrl(req.getImageUrl());
        entity.setLinkUrl(req.getLinkUrl() != null ? req.getLinkUrl() : "");
        if (req.getSortOrder() != null) {
            entity.setSortOrder(req.getSortOrder());
        }
        if (req.getValid() != null) {
            entity.setIsValid(req.getValid() ? 1 : 0);
        }
        if (entity.getId() == null) {
            if (entity.getSortOrder() == null) {
                entity.setSortOrder(0);
            }
            if (entity.getIsValid() == null) {
                entity.setIsValid(1);
            }
            bannerMapper.insert(entity);
        } else {
            bannerMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void delete(Long id) {
        requireExists(id);
        bannerMapper.deleteById(id);
    }

    @Override
    public void sort(List<SortItem> items) {
        for (SortItem item : items) {
            Banner patch = new Banner();
            patch.setId(item.getId());
            patch.setSortOrder(item.getSortOrder());
            bannerMapper.updateById(patch);
        }
    }

    private Banner requireExists(Long id) {
        Banner b = bannerMapper.selectById(id);
        if (b == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return b;
    }
}
