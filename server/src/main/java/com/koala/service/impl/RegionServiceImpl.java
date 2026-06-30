package com.koala.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.region.RegionNode;
import com.koala.entity.Region;
import com.koala.mapper.RegionMapper;
import com.koala.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;

    public RegionServiceImpl(RegionMapper regionMapper) {
        this.regionMapper = regionMapper;
    }

    @Override
    public List<RegionNode> listChildren(String parentCode) {
        List<Region> children;
        if (StringUtils.hasText(parentCode)) {
            children = regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                    .eq(Region::getParentCode, parentCode)
                    .orderByAsc(Region::getCode));
        } else {
            children = regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                    .eq(Region::getLevel, 1)
                    .orderByAsc(Region::getCode));
        }
        // 区/县(level=3)为叶子，无下级
        return children.stream()
                .map(r -> new RegionNode(r.getCode(), r.getName(), r.getLevel() != null && r.getLevel() < 3))
                .collect(Collectors.toList());
    }

    @Override
    public Region[] validateChain(String provinceCode, String cityCode, String districtCode) {
        Region province = regionMapper.selectById(provinceCode);
        Region city = regionMapper.selectById(cityCode);
        Region district = regionMapper.selectById(districtCode);

        boolean ok = province != null && city != null && district != null
                && province.getLevel() == 1 && city.getLevel() == 2 && district.getLevel() == 3
                && provinceCode.equals(city.getParentCode())
                && cityCode.equals(district.getParentCode());
        if (!ok) {
            throw new BizException(ErrorCode.ADDRESS_INVALID);
        }
        return new Region[]{province, city, district};
    }
}
