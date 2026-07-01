package com.koala.service;

import com.koala.dto.order.OrderItemRequest;
import com.koala.dto.order.PricingContext;

import java.util.List;

/** 算价服务(6.2)：预览与下单共用，保证预览=下单。券组合始终最优。 */
public interface PriceService {

    /**
     * 计算价格。
     * @param userId    用户
     * @param items     商品项(sku+qty)
     * @param withUpsell 是否计算凑单提示(仅预览需要)
     */
    PricingContext calculate(Long userId, List<OrderItemRequest> items, boolean withUpsell);
}
