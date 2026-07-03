package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.Admin;
import com.koala.mapper.AdminMapper;
import com.koala.repository.AdminRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AdminRepositoryImpl implements AdminRepository {

    private final AdminMapper adminMapper;

    public AdminRepositoryImpl(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Override
    public Admin findById(Long id) {
        return adminMapper.selectById(id);
    }

    @Override
    public Admin findByOpenid(String openid) {
        return adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getWxOpenid, openid));
    }

    @Override
    public boolean existsByOpenid(String openid) {
        Long c = adminMapper.selectCount(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getWxOpenid, openid));
        return c != null && c > 0;
    }

    @Override
    public List<Admin> findAll() {
        return adminMapper.selectList(Wrappers.<Admin>lambdaQuery()
                .orderByDesc(Admin::getId));
    }

    @Override
    public void insert(Admin admin) {
        adminMapper.insert(admin);
    }

    @Override
    public void updateById(Admin admin) {
        adminMapper.updateById(admin);
    }
}
