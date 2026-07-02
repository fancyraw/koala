package com.koala.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 对象存储配置(默认七牛;未配 accessKey 时走 mock 便于本地联调)。 */
@Data
@Component
@ConfigurationProperties(prefix = "koala.storage")
public class StorageProperties {

    /** provider: qiniu/oss/s3。 */
    private String provider = "qiniu";
    private String accessKey;
    private String secretKey;
    /** 存储空间名。 */
    private String bucket;
    /** CDN/访问域名(拼 publicUrl 用),如 https://cdn.example.com。 */
    private String domain;
}
