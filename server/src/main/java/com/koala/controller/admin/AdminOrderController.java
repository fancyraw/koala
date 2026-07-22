package com.koala.controller.admin;

import com.koala.common.result.PageResult;
import com.koala.common.result.Result;
import com.koala.dto.order.AdminOrderView;
import com.koala.dto.order.OrderRefundRequest;
import com.koala.dto.order.OrderShipRequest;
import com.koala.service.order.OrderQueryService;
import com.koala.service.order.OrderRefundService;
import com.koala.service.order.OrderShipService;
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

@Tag(name = "后台-订单")
@Validated
@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderQueryService queryService;
    private final OrderShipService shipService;
    private final OrderRefundService refundService;

    public AdminOrderController(OrderQueryService queryService, OrderShipService shipService,
                                OrderRefundService refundService) {
        this.queryService = queryService;
        this.shipService = shipService;
        this.refundService = refundService;
    }

    @Operation(summary = "订单列表(订单号/买家昵称/收货人搜索+状态+分页)")
    @GetMapping
    public Result<PageResult<AdminOrderView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 不能小于 1") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size 不能小于 1") @Max(value = 200, message = "size 不能大于 200") long size) {
        return Result.success(queryService.adminList(keyword, status, page, size));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/detail")
    public Result<AdminOrderView> detail(@RequestParam String no) {
        return Result.success(queryService.adminDetail(no));
    }

    @Operation(summary = "发货")
    @PostMapping("/ship")
    public Result<Void> ship(@Valid @RequestBody OrderShipRequest req) {
        shipService.ship(req);
        return Result.success();
    }

    @Operation(summary = "已完成订单手动发起退款")
    @PostMapping("/refund")
    public Result<Void> refund(@Valid @RequestBody OrderRefundRequest req) {
        refundService.adminRefund(req);
        return Result.success();
    }
}
