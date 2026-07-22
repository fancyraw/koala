package com.koala.controller;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.PageResult;
import com.koala.common.result.Result;
import com.koala.dto.order.OrderNoRequest;
import com.koala.dto.order.OrderPayRequest;
import com.koala.dto.order.OrderPreviewRequest;
import com.koala.dto.order.OrderPreviewView;
import com.koala.dto.order.OrderSubmitRequest;
import com.koala.dto.order.OrderSubmitView;
import com.koala.dto.order.OrderView;
import com.koala.infra.pay.PrepayResult;
import com.koala.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Tag(name = "C端-订单")
@Validated
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "算价预览(含凑单提示)")
    @PostMapping("/order/preview")
    public Result<OrderPreviewView> preview(@Valid @RequestBody OrderPreviewRequest req) {
        return Result.success(orderService.preview(AuthContext.requireUserId(), req));
    }

    @Operation(summary = "提交订单(锁资产,需 submitToken)")
    @PostMapping("/order/submit")
    public Result<OrderSubmitView> submit(@Valid @RequestBody OrderSubmitRequest req) {
        return Result.success(orderService.submit(AuthContext.requireUserId(), req));
    }

    @Operation(summary = "取支付参数")
    @PostMapping("/order/pay")
    public Result<PrepayResult> pay(@Valid @RequestBody OrderPayRequest req) {
        return Result.success(orderService.pay(AuthContext.requireUserId(), req.getNo()));
    }

    @Operation(summary = "支付渠道回调(验签幂等)")
    @PostMapping("/order/pay-notify")
    public Result<Void> payNotify(HttpServletRequest request) {
        boolean ok = orderService.payNotify(request);
        return ok ? Result.success() : Result.error(com.koala.common.result.ErrorCode.PAY_FAILED);
    }

    @Operation(summary = "我的订单(状态筛选/分页)")
    @GetMapping("/orders")
    public Result<PageResult<OrderView>> myOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 不能小于 1") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size 不能小于 1") @Max(value = 200, message = "size 不能大于 200") long size) {
        return Result.success(orderService.myOrders(AuthContext.requireUserId(), status, page, size));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/detail")
    public Result<OrderView> detail(@RequestParam String no) {
        return Result.success(orderService.detail(AuthContext.requireUserId(), no));
    }

    @Operation(summary = "取消订单(释放资产)")
    @PostMapping("/orders/cancel")
    public Result<Void> cancel(@Valid @RequestBody OrderNoRequest req) {
        orderService.cancel(AuthContext.requireUserId(), req.getNo());
        return Result.success();
    }

    @Operation(summary = "确认收货(幂等)")
    @PostMapping("/orders/confirm")
    public Result<Void> confirm(@Valid @RequestBody OrderNoRequest req) {
        orderService.confirm(AuthContext.requireUserId(), req.getNo());
        return Result.success();
    }

    @Operation(summary = "删除订单(软删)")
    @PostMapping("/orders/delete")
    public Result<Void> delete(@Valid @RequestBody OrderNoRequest req) {
        orderService.deleteByUser(AuthContext.requireUserId(), req.getNo());
        return Result.success();
    }
}
