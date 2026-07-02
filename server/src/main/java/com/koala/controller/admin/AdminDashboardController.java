package com.koala.controller.admin;

import com.koala.common.result.Result;
import com.koala.dto.dashboard.DashboardView;
import com.koala.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-数据看板")
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "看板聚合(今日概览+待处理+销售趋势+热销Top5,60s缓存)")
    @GetMapping
    public Result<DashboardView> overview(@RequestParam(defaultValue = "7") int range) {
        int rangeDays = range == 30 ? 30 : 7;
        return Result.success(dashboardService.overview(rangeDays));
    }
}
