package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.SysConfig;
import com.koala.mapper.SysConfigMapper;
import com.koala.repository.SysConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SysConfigRepositoryImpl implements SysConfigRepository {

    private final SysConfigMapper sysConfigMapper;

    public SysConfigRepositoryImpl(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    @Override
    public List<SysConfig> findAll() {
        return sysConfigMapper.selectList(null);
    }

    @Override
    public List<SysConfig> findByGroup(String group) {
        return sysConfigMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigGroup, group)
                .orderByAsc(SysConfig::getId));
    }

    @Override
    public SysConfig findByGroupAndKey(String group, String key) {
        return sysConfigMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigGroup, group)
                .eq(SysConfig::getConfigKey, key));
    }

    @Override
    public void insert(SysConfig config) {
        sysConfigMapper.insert(config);
    }

    @Override
    public void updateById(SysConfig config) {
        sysConfigMapper.updateById(config);
    }
}
