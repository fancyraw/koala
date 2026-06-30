package com.koala.service;

import com.koala.dto.region.RegionNode;
import com.koala.entity.Region;

import java.util.List;

public interface RegionService {

    /** 取某父级下的子区划；parent 为空取省级。供 C 端联动选择器。 */
    List<RegionNode> listChildren(String parentCode);

    /**
     * 校验省/市/区三级码合法且层级正确（区.parent=市、市.parent=省）。
     * 合法返回三级实体（用于回填中文名/完整地址），非法抛 BizException。
     */
    Region[] validateChain(String provinceCode, String cityCode, String districtCode);
}
