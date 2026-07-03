package com.koala.service.impl;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.region.RegionNode;
import com.koala.entity.Region;
import com.koala.enums.RegionLevel;
import com.koala.repository.RegionRepository;
import com.koala.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;

    public RegionServiceImpl(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    public List<RegionNode> listChildren(String parentCode) {
        List<Region> children = StringUtils.hasText(parentCode)
                ? regionRepository.findByParent(parentCode)
                : regionRepository.findByLevel(RegionLevel.PROVINCE.code());
        // 区/县(level=3)为叶子，无下级
        return children.stream()
                .map(r -> new RegionNode(r.getCode(), r.getName(),
                        !RegionLevel.DISTRICT.is(r.getLevel())))
                .collect(Collectors.toList());
    }

    @Override
    public Region[] validateChain(String provinceCode, String cityCode, String districtCode) {
        Region province = regionRepository.findByCode(provinceCode);
        Region city = regionRepository.findByCode(cityCode);
        Region district = regionRepository.findByCode(districtCode);

        boolean ok = province != null && city != null && district != null
                && RegionLevel.PROVINCE.is(province.getLevel())
                && RegionLevel.CITY.is(city.getLevel())
                && RegionLevel.DISTRICT.is(district.getLevel())
                && provinceCode.equals(city.getParentCode())
                && cityCode.equals(district.getParentCode());
        if (!ok) {
            throw new BizException(ErrorCode.ADDRESS_INVALID);
        }
        return new Region[]{province, city, district};
    }
}
