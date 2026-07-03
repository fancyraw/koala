package com.koala.repository;

import com.koala.entity.Region;

import java.util.List;

public interface RegionRepository {

    /** 按行政区划编码查询,不存在返回 null。 */
    Region findByCode(String code);

    /** 指定父级下的子区划,按 code 升序。 */
    List<Region> findByParent(String parentCode);

    /** 指定层级的区划,按 code 升序。 */
    List<Region> findByLevel(int level);
}
