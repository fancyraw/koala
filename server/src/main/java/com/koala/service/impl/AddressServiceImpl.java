package com.koala.service.impl;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.address.AddressSaveRequest;
import com.koala.dto.address.AddressView;
import com.koala.entity.Region;
import com.koala.entity.UserAddress;
import com.koala.enums.ValidFlag;
import com.koala.repository.UserAddressRepository;
import com.koala.service.AddressService;
import com.koala.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    private final UserAddressRepository addressRepository;
    private final RegionService regionService;

    public AddressServiceImpl(UserAddressRepository addressRepository, RegionService regionService) {
        this.addressRepository = addressRepository;
        this.regionService = regionService;
    }

    @Override
    public List<AddressView> list(Long userId) {
        return addressRepository.findByUser(userId).stream()
                .map(AddressView::of).collect(Collectors.toList());
    }

    @Override
    public AddressView detail(Long userId, Long id) {
        return AddressView.of(requireOwned(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(Long userId, AddressSaveRequest req) {
        UserAddress entity = new UserAddress();
        entity.setUserId(userId);
        fill(entity, req);

        boolean asDefault = Boolean.TRUE.equals(req.getIsDefault())
                || addressRepository.countByUser(userId) == 0;
        if (asDefault) {
            addressRepository.clearDefault(userId);
        }
        entity.setIsDefault(ValidFlag.of(asDefault));
        addressRepository.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, AddressSaveRequest req) {
        if (req.getId() == null) {
            throw new BizException(ErrorCode.PARAM_MISSING);
        }
        UserAddress entity = requireOwned(userId, req.getId());
        fill(entity, req);
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            addressRepository.clearDefault(userId);
            entity.setIsDefault(ValidFlag.ENABLED.code());
        }
        addressRepository.updateById(entity);
    }

    @Override
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        addressRepository.deleteById(id);
    }

    private UserAddress requireOwned(Long userId, Long id) {
        UserAddress entity = addressRepository.findById(id);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return entity;
    }

    /** 写入基础字段并校验/回填三级区划。 */
    private void fill(UserAddress entity, AddressSaveRequest req) {
        Region[] chain = regionService.validateChain(req.getProvinceCode(), req.getCityCode(), req.getDistrictCode());
        Region province = chain[0];
        Region city = chain[1];
        Region district = chain[2];

        entity.setName(req.getName());
        entity.setPhone(req.getPhone());
        entity.setProvinceCode(province.getCode());
        entity.setCityCode(city.getCode());
        entity.setDistrictCode(district.getCode());
        entity.setProvince(province.getName());
        entity.setCity(city.getName());
        entity.setDistrict(district.getName());
        entity.setDetail(req.getDetail());
        entity.setFullAddress(province.getName() + city.getName() + district.getName() + req.getDetail());
    }
}
