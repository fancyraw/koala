package com.koala.infra.storage;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.json.JSONObject;
import com.koala.common.result.ErrorCode;
import com.koala.common.exception.BizException;
import com.koala.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 七牛对象存储实现(默认 provider)。前端直传见 6.8：后端只签发凭证不中转文件。
 * 未配 accessKey 时走 mock,便于本地联调;配好后自动切真实上传 token。
 * 切 OSS/S3 只需新增实现并改 koala.storage.provider(见 3.2 扩展点)。
 */
@Slf4j
@Component
public class QiniuStorage implements FileStorage {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long EXPIRE_SECONDS = 300L;
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final String MIME_LIMIT = "image/jpeg;image/png;image/webp";
    private static final String UPLOAD_URL = "https://upload.qiniup.com";

    private final StorageProperties props;

    public QiniuStorage(StorageProperties props) {
        this.props = props;
    }

    @Override
    public String provider() {
        return "qiniu";
    }

    @Override
    public UploadCredential issueCredential(String bizType) {
        String biz = StrUtil.isBlank(bizType) ? "misc" : bizType.replaceAll("[^a-zA-Z0-9_-]", "");
        if (StrUtil.isBlank(biz)) {
            biz = "misc";
        }
        String key = "koala/" + biz + "/" + LocalDate.now().format(DATE_FMT) + "/" + IdUtil.fastSimpleUUID();

        UploadCredential cred = new UploadCredential();
        cred.setProvider(provider());
        cred.setUploadUrl(UPLOAD_URL);
        cred.setKey(key);
        cred.setPublicUrl(publicUrl(key));
        cred.setExpireSeconds(EXPIRE_SECONDS);
        cred.getParams().put("mimeLimit", MIME_LIMIT);
        cred.getParams().put("fsizeLimit", String.valueOf(MAX_SIZE));

        String token = StrUtil.isBlank(props.getAccessKey())
                ? "mock_qiniu_token_" + IdUtil.fastSimpleUUID()
                : buildUploadToken(key);
        cred.getParams().put("token", token);
        return cred;
    }

    @Override
    public String publicUrl(String key) {
        String domain = StrUtil.isBlank(props.getDomain()) ? "https://cdn.example.com" : props.getDomain();
        return StrUtil.removeSuffix(domain, "/") + "/" + key;
    }

    @Override
    public void delete(String key) {
        if (StrUtil.isBlank(props.getAccessKey())) {
            log.info("[mock] delete object key={}", key);
            return;
        }
        // 真实删除需调七牛管理 API(bucket/key 的 base64url + 管理凭证),生产接入时补全
        throw new BizException(ErrorCode.SYSTEM_ERROR, "七牛真实删除未接入");
    }

    /** 构造七牛直传 token：accessKey:base64url(HMAC-SHA1(sk, encodedPolicy)):encodedPolicy。 */
    private String buildUploadToken(String key) {
        JSONObject policy = new JSONObject();
        policy.set("scope", props.getBucket() + ":" + key);
        policy.set("deadline", System.currentTimeMillis() / 1000 + EXPIRE_SECONDS);
        policy.set("insertOnly", 1);
        policy.set("fsizeLimit", MAX_SIZE);
        policy.set("mimeLimit", MIME_LIMIT);
        policy.set("returnBody", "{\"key\":\"$(key)\"}");

        String encodedPolicy = base64Url(policy.toString().getBytes(StandardCharsets.UTF_8));
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA1, props.getSecretKey().getBytes(StandardCharsets.UTF_8));
        String sign = base64Url(hmac.digest(encodedPolicy));
        return props.getAccessKey() + ":" + sign + ":" + encodedPolicy;
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }
}
