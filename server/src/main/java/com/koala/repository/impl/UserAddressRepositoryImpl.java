package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.UserAddress;
import com.koala.enums.ValidFlag;
import com.koala.mapper.UserAddressMapper;
import com.koala.repository.UserAddressRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserAddressRepositoryImpl implements UserAddressRepository {

    private final UserAddressMapper addressMapper;

    public UserAddressRepositoryImpl(UserAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public UserAddress findById(Long id) {
        return addressMapper.selectById(id);
    }

    @Override
    public List<UserAddress> findByUser(Long userId) {
        return addressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId));
    }

    @Override
    public long countByUser(Long userId) {
        Long c = addressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId));
        return c == null ? 0 : c;
    }

    @Override
    public void insert(UserAddress address) {
        addressMapper.insert(address);
    }

    @Override
    public void updateById(UserAddress address) {
        addressMapper.updateById(address);
    }

    @Override
    public void deleteById(Long id) {
        addressMapper.deleteById(id);
    }

    @Override
    public void clearDefault(Long userId) {
        UserAddress patch = new UserAddress();
        patch.setIsDefault(ValidFlag.DISABLED.code());
        addressMapper.update(patch, Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, ValidFlag.ENABLED.code()));
    }
}
