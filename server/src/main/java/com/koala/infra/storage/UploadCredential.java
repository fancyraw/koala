package com.koala.infra.storage;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 直传凭证：前端据此直传对象存储，不经后端中转(见 6.8)。
 * 具体字段依 provider 而定(七牛=uploadToken,OSS/S3=policy+signature),统一放 params。
 */
@Data
public class UploadCredential {

    /** 存储 provider(qiniu/oss/s3)。 */
    private String provider;
    /** 直传目标 URL(前端 POST 到此)。 */
    private String uploadUrl;
    /** 授权写入的对象 key(受控前缀 koala/{bizType}/{date}/{uuid})。 */
    private String key;
    /** 回存业务库/展示用的最终访问 URL。 */
    private String publicUrl;
    /** 凭证有效秒数(≤300)。 */
    private long expireSeconds;
    /** provider 相关的直传参数(uploadToken/policy/signature/content-length-range 等)。 */
    private Map<String, String> params = new HashMap<>();
}
