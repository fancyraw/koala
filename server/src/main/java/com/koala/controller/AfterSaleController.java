package com.koala.controller;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.PageResult;
import com.koala.common.result.Result;
import com.koala.dto.aftersale.AfterSaleApplyRequest;
import com.koala.dto.aftersale.AfterSaleNoRequest;
import com.koala.dto.aftersale.AfterSaleTrackingRequest;
import com.koala.dto.aftersale.AfterSaleView;
import com.koala.service.AfterSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "C端-售后")
@RestController
@RequestMapping("/after-sales")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    public AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @Operation(summary = "申请售后(待发货→仅退款/待收货→退货退款)")
    @PostMapping("/apply")
    public Result<String> apply(@Valid @RequestBody AfterSaleApplyRequest req) {
        return Result.success(afterSaleService.apply(AuthContext.requireUserId(), req));
    }

    @Operation(summary = "撤销申请(仅待审核)")
    @PostMapping("/cancel")
    public Result<Void> cancel(@Valid @RequestBody AfterSaleNoRequest req) {
        afterSaleService.cancelByUser(AuthContext.requireUserId(), req.getAfterSaleNo());
        return Result.success();
    }

    @Operation(summary = "填写/修改寄回单号(退货退款)")
    @PostMapping("/tracking")
    public Result<Void> tracking(@Valid @RequestBody AfterSaleTrackingRequest req) {
        afterSaleService.fillTracking(AuthContext.requireUserId(), req);
        return Result.success();
    }

    @Operation(summary = "我的售后(状态筛选/分页)")
    @GetMapping
    public Result<PageResult<AfterSaleView>> myList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(afterSaleService.myList(AuthContext.requireUserId(), status, page, size));
    }

    @Operation(summary = "售后详情/进度")
    @GetMapping("/detail")
    public Result<AfterSaleView> detail(@RequestParam String afterSaleNo) {
        return Result.success(afterSaleService.detail(AuthContext.requireUserId(), afterSaleNo));
    }
}
