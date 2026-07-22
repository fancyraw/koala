package com.koala.dto.admin;

import com.koala.entity.SysConfig;
import lombok.Data;

@Data
public class SysConfigView {

    private String configGroup;
    private String configKey;
    private String configValue;
    private String remark;

    public static SysConfigView from(SysConfig c) {
        SysConfigView v = new SysConfigView();
        v.setConfigGroup(c.getConfigGroup());
        v.setConfigKey(c.getConfigKey());
        v.setConfigValue(c.getConfigValue());
        v.setRemark(c.getRemark());
        return v;
    }
}
