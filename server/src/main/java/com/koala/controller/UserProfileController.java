package com.koala.controller;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.Result;
import com.koala.dto.user.ProfileUpdateRequest;
import com.koala.dto.user.ProfileView;
import com.koala.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "C端-个人资料")
@RestController
@RequestMapping("/user")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "查询个人资料")
    @GetMapping("/profile")
    public Result<ProfileView> profile() {
        return Result.success(userService.profile(AuthContext.requireUserId()));
    }

    @Operation(summary = "更新昵称/头像")
    @PostMapping("/profile/update")
    public Result<ProfileView> update(@Valid @RequestBody ProfileUpdateRequest req) {
        return Result.success(userService.updateProfile(AuthContext.requireUserId(), req));
    }
}
