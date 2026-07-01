package com.koala.dto.content;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/** 后台保存某分组下若干配置项(key->value)。 */
@Data
public class ConfigSaveRequest {

    @NotBlank(message = "配置分组不能为空")
    private String group;

    @NotEmpty(message = "配置项不能为空")
    @Valid
    private List<ConfigItem> items;

    @Data
    public static class ConfigItem {

        @NotBlank(message = "配置键不能为空")
        private String key;

        private String value;
    }
}
