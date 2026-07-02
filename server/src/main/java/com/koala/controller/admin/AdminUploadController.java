package com.koala.controller.admin;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.Result;
import com.koala.infra.storage.FileStorage;
import com.koala.infra.storage.UploadCredential;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台图片上传(见 6.8)：签发前端直传凭证,前端直传对象存储,回存 CDN URL,不经后端中转。
 * 凭证短 TTL(≤5min)、图片类型白名单、≤5MB、受控前缀 koala/{bizType}/{date}/。
 */
@Tag(name = "后台-图片上传")
@RestController
@RequestMapping("/admin/upload")
public class AdminUploadController {

    private final FileStorage fileStorage;

    public AdminUploadController(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Operation(summary = "签发前端直传凭证(bizType 决定存储前缀,如 product/banner)")
    @GetMapping("/credential")
    public Result<UploadCredential> credential(@RequestParam(defaultValue = "misc") String bizType) {
        AuthContext.requireAdminId();
        return Result.success(fileStorage.issueCredential(bizType));
    }
}
