package com.koala.controller.admin;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.Result;
import com.koala.dto.admin.AcceptInviteRequest;
import com.koala.dto.admin.AdminStatusRequest;
import com.koala.dto.admin.AdminView;
import com.koala.dto.admin.InviteResponse;
import com.koala.service.AdminManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/** 管理员管理(仅超管,切面校验 /admin/admins/** 的 isSuper;唯 /accept 免登录)。见 6.10。 */
@Tag(name = "后台-管理员")
@RestController
@RequestMapping("/admin/admins")
public class AdminManageController {

    private final AdminManageService adminManageService;

    public AdminManageController(AdminManageService adminManageService) {
        this.adminManageService = adminManageService;
    }

    @Operation(summary = "管理员列表(含待审核)")
    @GetMapping
    public Result<List<AdminView>> list() {
        return Result.success(adminManageService.list());
    }

    @Operation(summary = "生成一次性邀请二维码/链接")
    @PostMapping("/invite")
    public Result<InviteResponse> invite() {
        return Result.success(adminManageService.invite());
    }

    @Operation(summary = "新人扫码入库(免登录,建号待审核)")
    @PostMapping("/accept")
    public Result<Void> accept(@Valid @RequestBody AcceptInviteRequest req) {
        adminManageService.accept(req);
        return Result.success();
    }

    @Operation(summary = "启用/禁用管理员")
    @PostMapping("/status")
    public Result<Void> status(@Valid @RequestBody AdminStatusRequest req) {
        adminManageService.setStatus(req.getId(), req.getValid(), AuthContext.requireAdminId());
        return Result.success();
    }
}
