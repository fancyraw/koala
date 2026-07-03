package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.Region;
import com.koala.mapper.RegionMapper;
import com.koala.repository.RegionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RegionRepositoryImpl implements RegionRepository {

    private final RegionMapper regionMapper;

    public RegionRepositoryImpl(RegionMapper regionMapper) {
        this.regionMapper = regionMapper;
    }

    @Override
    public Region findByCode(String code) {
        return regionMapper.selectById(code);
    }

    @Override
    public List<Region> findByParent(String parentCode) {
        return regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                .eq(Region::getParentCode, parentCode)
                .orderByAsc(Region::getCode));
    }

    @Override
    public List<Region> findByLevel(int level) {
        return regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                .eq(Region::getLevel, level)
                .orderByAsc(Region::getCode));
    }
}
