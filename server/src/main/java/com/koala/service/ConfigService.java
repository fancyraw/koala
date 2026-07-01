package com.koala.service;

import com.koala.entity.SysConfig;

import java.math.BigDecimal;
import java.util.List;

/** 系统配置：启动加载入本地缓存，保存后刷新。改策略不发版。 */
public interface ConfigService {

    String get(String group, String key, String defaultValue);

    int getInt(String group, String key, int defaultValue);

    BigDecimal getDecimal(String group, String key, BigDecimal defaultValue);

    /** 后台读取某分组全部配置。 */
    List<SysConfig> listByGroup(String group);

    /** 后台保存(key->value)并刷新缓存。 */
    void save(String group, String key, String value, Long adminId);

    /** 强制重载缓存。 */
    void reload();
}
