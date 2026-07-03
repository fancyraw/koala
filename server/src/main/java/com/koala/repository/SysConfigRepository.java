package com.koala.repository;

import com.koala.entity.SysConfig;

import java.util.List;

public interface SysConfigRepository {

    /** 全部配置项。 */
    List<SysConfig> findAll();

    /** 按分组查询,按 id 升序。 */
    List<SysConfig> findByGroup(String group);

    /** 按分组 + 键查询单条,不存在返回 null。 */
    SysConfig findByGroupAndKey(String group, String key);

    void insert(SysConfig config);

    void updateById(SysConfig config);
}
