package com.koala.service.impl;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.content.BannerSaveRequest;
import com.koala.dto.content.BannerView;
import com.koala.dto.product.SortItem;
import com.koala.entity.Banner;
import com.koala.enums.ValidFlag;
import com.koala.repository.BannerRepository;
import com.koala.service.BannerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    public BannerServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Override
    public List<BannerView> listValid() {
        return bannerRepository.findEnabled()
                .stream().map(BannerView::of).collect(Collectors.toList());
    }

    @Override
    public List<BannerView> listAll() {
        return bannerRepository.findAll()
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
            entity.setIsValid(ValidFlag.of(req.getValid()));
        }
        if (entity.getId() == null) {
            if (entity.getSortOrder() == null) {
                entity.setSortOrder(0);
            }
            if (entity.getIsValid() == null) {
                entity.setIsValid(ValidFlag.ENABLED.code());
            }
            bannerRepository.insert(entity);
        } else {
            bannerRepository.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void delete(Long id) {
        requireExists(id);
        bannerRepository.deleteById(id);
    }

    @Override
    public void sort(List<SortItem> items) {
        for (SortItem item : items) {
            Banner patch = new Banner();
            patch.setId(item.getId());
            patch.setSortOrder(item.getSortOrder());
            bannerRepository.updateById(patch);
        }
    }

    private Banner requireExists(Long id) {
        Banner b = bannerRepository.findById(id);
        if (b == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return b;
    }
}
