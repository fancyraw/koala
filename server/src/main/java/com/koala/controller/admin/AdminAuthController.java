package com.koala.controller.admin;

import com.koala.common.auth.JwtUtil;
import com.koala.common.auth.TokenBlacklist;
import com.koala.common.result.Result;
import com.koala.dto.auth.QrcodeCheckResponse;
import com.koala.dto.auth.QrcodeResponse;
import com.koala.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Tag(name = "后台-登录")
@RestController
@RequestMapping("/admin/login")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist blacklist;

    public AdminAuthController(AdminAuthService adminAuthService, JwtUtil jwtUtil, TokenBlacklist blacklist) {
        this.adminAuthService = adminAuthService;
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
    }

    @Operation(summary = "取扫码二维码")
    @GetMapping("/qrcode")
    public Result<QrcodeResponse> qrcode() {
        return Result.success(adminAuthService.createQrcode());
    }

    @Operation(summary = "轮询扫码结果")
    @PostMapping("/check")
    public Result<QrcodeCheckResponse> check(@RequestParam String state) {
        return Result.success(adminAuthService.check(state));
    }

    @Operation(summary = "微信扫码回调")
    @GetMapping("/callback")
    public Result<Void> callback(@RequestParam String state, @RequestParam String code) {
        adminAuthService.handleCallback(state, code);
        return Result.success();
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            JwtUtil.ParsedToken parsed = jwtUtil.parse(header.substring(7));
            if (parsed != null) {
                blacklist.revoke(parsed.jti, parsed.remainingSeconds());
            }
        }
        return Result.success();
    }
}
