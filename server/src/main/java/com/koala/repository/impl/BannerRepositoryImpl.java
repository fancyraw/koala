package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.Banner;
import com.koala.enums.ValidFlag;
import com.koala.mapper.BannerMapper;
import com.koala.repository.BannerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BannerRepositoryImpl implements BannerRepository {

    private final BannerMapper bannerMapper;

    public BannerRepositoryImpl(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    @Override
    public Banner findById(Long id) {
        return bannerMapper.selectById(id);
    }

    @Override
    public List<Banner> findAll() {
        return bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                .orderByAsc(Banner::getSortOrder)
                .orderByAsc(Banner::getId));
    }

    @Override
    public List<Banner> findEnabled() {
        return bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                .eq(Banner::getIsValid, ValidFlag.ENABLED.code())
                .orderByAsc(Banner::getSortOrder)
                .orderByAsc(Banner::getId));
    }

    @Override
    public void insert(Banner banner) {
        bannerMapper.insert(banner);
    }

    @Override
    public void updateById(Banner banner) {
        bannerMapper.updateById(banner);
    }

    @Override
    public void deleteById(Long id) {
        bannerMapper.deleteById(id);
    }
}
