package com.koala.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "koala.wechat")
public class WechatProperties {

    /** C 端小程序 */
    private Mp mp = new Mp();
    /** 后台开放平台扫码 */
    private Open open = new Open();
    /**
     * 允许在配置缺失时走 mock 路径（登录 / 支付回调）。
     * 默认关闭；仅 application-dev.yml 显式开启，防止误在 prod 生效。
     */
    private boolean mockWhenUnconfigured = false;

    @Data
    public static class Mp {
        private String appid;
        private String secret;
    }

    @Data
    public static class Open {
        private String appid;
        private String secret;
    }
}
