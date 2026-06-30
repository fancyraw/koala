package com.koala.dto.region;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegionNode {

    private String code;
    private String name;
    /** 是否有下级（区/县为叶子，前端据此决定是否继续联动） */
    private boolean hasChildren;
}
