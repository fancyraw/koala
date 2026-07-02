package com.koala.infra.storage;

/**
 * 文件存储抽象(扩展点,见 3.2)：默认 QiniuStorage，切 OSS/S3 改 koala.storage.provider 即可。
 * 上传走前端直传(issueCredential),后端只发凭证不中转文件。
 */
public interface FileStorage {

    String provider();

    /** 签发前端直传凭证：短 TTL(≤5min)、图片类型白名单、≤5MB、受控前缀。 */
    UploadCredential issueCredential(String bizType);

    /** 由对象 key 拼访问 URL。 */
    String publicUrl(String key);

    /** 删除对象。 */
    void delete(String key);
}
