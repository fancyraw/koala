package com.koala.controller;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.Result;
import com.koala.infra.storage.FileStorage;
import com.koala.infra.storage.UploadCredential;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端图片上传：签发前端直传凭证，前端直传对象存储不经后端中转(见 6.8)。
 * 需 C 端登录；bizType 固定 aftersale(仅售后凭证)，避免占用后台受控前缀。
 */
@Tag(name = "C端-图片上传")
@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final String BIZ_TYPE = "aftersale";

    private final FileStorage fileStorage;

    public UploadController(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Operation(summary = "签发售后凭证直传凭证(C端)")
    @GetMapping("/credential")
    public Result<UploadCredential> credential() {
        AuthContext.requireUserId();
        return Result.success(fileStorage.issueCredential(BIZ_TYPE));
    }
}
