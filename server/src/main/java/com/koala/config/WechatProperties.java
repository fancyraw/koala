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
    /** dev 模式：appid 为空时允许用 code 直接当 openid，便于本地联调 */
    private boolean mockWhenUnconfigured = true;

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
