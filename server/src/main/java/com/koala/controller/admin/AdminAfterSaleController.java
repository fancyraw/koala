package com.koala.controller.admin;

import com.koala.common.result.PageResult;
import com.koala.common.result.Result;
import com.koala.dto.aftersale.AdminAfterSaleView;
import com.koala.dto.aftersale.AfterSaleAuditRequest;
import com.koala.dto.aftersale.AfterSaleNoRequest;
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

@Tag(name = "后台-售后")
@RestController
@RequestMapping("/admin/after-sales")
public class AdminAfterSaleController {

    private final AfterSaleService afterSaleService;

    public AdminAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @Operation(summary = "售后列表(售后单号/订单号/买家昵称搜索+状态+分页)")
    @GetMapping
    public Result<PageResult<AdminAfterSaleView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(afterSaleService.adminList(keyword, status, page, size));
    }

    @Operation(summary = "售后详情")
    @GetMapping("/detail")
    public Result<AdminAfterSaleView> detail(@RequestParam String afterSaleNo) {
        return Result.success(afterSaleService.adminDetail(afterSaleNo));
    }

    @Operation(summary = "审核(同意/拒绝)")
    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody AfterSaleAuditRequest req) {
        afterSaleService.audit(req);
        return Result.success();
    }

    @Operation(summary = "确认收货并退款(退货退款)")
    @PostMapping("/confirm-receive")
    public Result<Void> confirmReceive(@Valid @RequestBody AfterSaleNoRequest req) {
        afterSaleService.confirmReceive(req.getAfterSaleNo());
        return Result.success();
    }
}
