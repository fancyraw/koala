package com.koala.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.SysConfig;
import com.koala.mapper.SysConfigMapper;
import com.koala.service.ConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfigServiceImpl implements ConfigService {

    private final SysConfigMapper sysConfigMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public ConfigServiceImpl(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    @PostConstruct
    @Override
    public void reload() {
        Map<String, String> fresh = new ConcurrentHashMap<>();
        for (SysConfig c : sysConfigMapper.selectList(null)) {
            fresh.put(cacheKey(c.getConfigGroup(), c.getConfigKey()), c.getConfigValue());
        }
        cache.clear();
        cache.putAll(fresh);
    }

    @Override
    public String get(String group, String key, String defaultValue) {
        String v = cache.get(cacheKey(group, key));
        return v != null ? v : defaultValue;
    }

    @Override
    public int getInt(String group, String key, int defaultValue) {
        String v = cache.get(cacheKey(group, key));
        if (v == null || v.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public BigDecimal getDecimal(String group, String key, BigDecimal defaultValue) {
        String v = cache.get(cacheKey(group, key));
        if (v == null || v.isEmpty()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<SysConfig> listByGroup(String group) {
        return sysConfigMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigGroup, group)
                .orderByAsc(SysConfig::getId));
    }

    @Override
    public void save(String group, String key, String value, Long adminId) {
        SysConfig existing = sysConfigMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigGroup, group)
                .eq(SysConfig::getConfigKey, key));
        if (existing == null) {
            SysConfig c = new SysConfig();
            c.setConfigGroup(group);
            c.setConfigKey(key);
            c.setConfigValue(value);
            c.setUpdatedBy(adminId != null ? adminId : 0L);
            sysConfigMapper.insert(c);
        } else {
            existing.setConfigValue(value);
            existing.setUpdatedBy(adminId != null ? adminId : 0L);
            existing.setUpdatedAt(LocalDateTime.now());
            sysConfigMapper.updateById(existing);
        }
        cache.put(cacheKey(group, key), value);
    }

    private static String cacheKey(String group, String key) {
        return group + ":" + key;
    }
}
