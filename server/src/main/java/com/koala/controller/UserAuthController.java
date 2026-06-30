package com.koala.controller;

import com.koala.common.auth.JwtUtil;
import com.koala.common.auth.TokenBlacklist;
import com.koala.common.result.Result;
import com.koala.dto.auth.LoginResponse;
import com.koala.dto.auth.UserLoginRequest;
import com.koala.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Tag(name = "C端-登录")
@RestController
@RequestMapping("/user")
public class UserAuthController {

    private final UserAuthService userAuthService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist blacklist;

    public UserAuthController(UserAuthService userAuthService, JwtUtil jwtUtil, TokenBlacklist blacklist) {
        this.userAuthService = userAuthService;
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
    }

    @Operation(summary = "微信登录/注册")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody UserLoginRequest req) {
        return Result.success(userAuthService.loginByWechat(req.getCode()));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        revokeCurrent(request);
        return Result.success();
    }

    private void revokeCurrent(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            JwtUtil.ParsedToken parsed = jwtUtil.parse(header.substring(7));
            if (parsed != null) {
                blacklist.revoke(parsed.jti, parsed.remainingSeconds());
            }
        }
    }
}
