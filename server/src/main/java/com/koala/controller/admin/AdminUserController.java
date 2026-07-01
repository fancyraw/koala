package com.koala.controller.admin;

import com.koala.common.result.PageResult;
import com.koala.common.result.Result;
import com.koala.dto.user.AdminUserDetailView;
import com.koala.dto.user.AdminUserView;
import com.koala.dto.user.UserStatusRequest;
import com.koala.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "后台-用户")
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户列表(昵称模糊/状态过滤)")
    @GetMapping
    public Result<PageResult<AdminUserView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(userService.listForAdmin(keyword, status, page, size));
    }

    @Operation(summary = "用户详情(含下单统计)")
    @GetMapping("/detail")
    public Result<AdminUserDetailView> detail(@RequestParam Long id) {
        return Result.success(userService.detailForAdmin(id));
    }

    @Operation(summary = "禁用/启用用户")
    @PostMapping("/status")
    public Result<Void> status(@Valid @RequestBody UserStatusRequest req) {
        userService.setStatus(req.getId(), req.getValid());
        return Result.success();
    }
}
