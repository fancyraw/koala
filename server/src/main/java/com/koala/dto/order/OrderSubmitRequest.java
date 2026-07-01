package com.koala.dto.order;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/** 提交订单：商品项 + 收货地址 + 幂等 token(SETNX)。券组合始终最优，无需前端选券。 */
@Data
public class OrderSubmitRequest {

    @Valid
    @NotEmpty(message = "商品项不能为空")
    private List<OrderItemRequest> items;

    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    @NotNull(message = "缺少提交令牌")
    private String submitToken;

    private String remark;
}
