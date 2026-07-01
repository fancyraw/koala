package com.koala.controller;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.Result;
import com.koala.dto.coupon.GrantResultView;
import com.koala.dto.coupon.UserCouponView;
import com.koala.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "C端-优惠券")
@RestController
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(summary = "自动下发(把可领且未领的券各发1张，幂等)")
    @PostMapping("/auto-grant")
    public Result<GrantResultView> autoGrant() {
        return Result.success(couponService.autoGrant(AuthContext.requireUserId()));
    }

    @Operation(summary = "我的券(status 可空/0未使用/1已使用/2已过期)")
    @GetMapping("/mine")
    public Result<List<UserCouponView>> mine(@RequestParam(required = false) Integer status) {
        return Result.success(couponService.mine(AuthContext.requireUserId(), status));
    }
}
