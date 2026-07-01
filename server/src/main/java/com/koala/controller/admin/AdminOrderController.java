package com.koala.controller.admin;

import com.koala.common.result.PageResult;
import com.koala.common.result.Result;
import com.koala.dto.order.AdminOrderView;
import com.koala.dto.order.OrderRefundRequest;
import com.koala.dto.order.OrderShipRequest;
import com.koala.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "后台-订单")
@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "订单列表(订单号/买家昵称/收货人搜索+状态+分页)")
    @GetMapping
    public Result<PageResult<AdminOrderView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(orderService.adminList(keyword, status, page, size));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/detail")
    public Result<AdminOrderView> detail(@RequestParam String no) {
        return Result.success(orderService.adminDetail(no));
    }

    @Operation(summary = "发货")
    @PostMapping("/ship")
    public Result<Void> ship(@Valid @RequestBody OrderShipRequest req) {
        orderService.ship(req);
        return Result.success();
    }

    @Operation(summary = "已完成订单手动发起退款")
    @PostMapping("/refund")
    public Result<Void> refund(@Valid @RequestBody OrderRefundRequest req) {
        orderService.adminRefund(req);
        return Result.success();
    }
}
