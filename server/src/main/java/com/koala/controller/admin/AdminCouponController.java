package com.koala.controller.admin;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.PageResult;
import com.koala.common.result.Result;
import com.koala.common.web.IdRequest;
import com.koala.dto.coupon.AdminCouponView;
import com.koala.dto.coupon.CouponSaveRequest;
import com.koala.dto.coupon.GrantDetailView;
import com.koala.service.AdminCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@Tag(name = "后台-优惠券")
@Validated
@RestController
@RequestMapping("/admin/coupons")
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    public AdminCouponController(AdminCouponService adminCouponService) {
        this.adminCouponService = adminCouponService;
    }

    @Operation(summary = "券模板列表(按派生态过滤/分页)")
    @GetMapping
    public Result<PageResult<AdminCouponView>> list(
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 不能小于 1") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size 不能小于 1") @Max(value = 200, message = "size 不能大于 200") long size) {
        return Result.success(adminCouponService.list(state, page, size));
    }

    @Operation(summary = "券详情")
    @GetMapping("/detail")
    public Result<AdminCouponView> detail(@RequestParam Long id) {
        return Result.success(adminCouponService.detail(id));
    }

    @Operation(summary = "新增/编辑券模板")
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody CouponSaveRequest req) {
        return Result.success(adminCouponService.save(req, AuthContext.requireAdminId()));
    }

    @Operation(summary = "停发(终态不可逆)")
    @PostMapping("/stop")
    public Result<Void> stop(@Valid @RequestBody IdRequest req) {
        adminCouponService.stop(req.getId());
        return Result.success();
    }

    @Operation(summary = "删除(仅无下发记录可删)")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody IdRequest req) {
        adminCouponService.delete(req.getId());
        return Result.success();
    }

    @Operation(summary = "券发放明细")
    @GetMapping("/grants")
    public Result<List<GrantDetailView>> grants(@RequestParam Long id) {
        return Result.success(adminCouponService.grants(id));
    }
}
