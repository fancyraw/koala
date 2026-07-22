package com.koala.converter;

import com.koala.dto.order.AdminOrderView;
import com.koala.dto.order.OrderItemView;
import com.koala.dto.order.OrderView;
import com.koala.entity.Order;
import com.koala.entity.OrderItem;

import java.util.List;
import java.util.stream.Collectors;

/** 订单 entity → 视图。纯字段拷贝，无 DB / 无鉴权。 */
public final class OrderConverter {

    private OrderConverter() {}

    public static OrderView toView(Order o, List<OrderItem> items) {
        OrderView v = new OrderView();
        v.setOrderNo(o.getOrderNo());
        v.setUserId(o.getUserId());
        v.setReceiverName(o.getReceiverName());
        v.setReceiverPhone(o.getReceiverPhone());
        v.setReceiverAddress(o.getReceiverAddress());
        v.setProductAmount(o.getProductAmount());
        v.setCouponDiscount(o.getCouponDiscount());
        v.setShippingFee(o.getShippingFee());
        v.setPayAmount(o.getPayAmount());
        v.setLogisticsCompany(o.getLogisticsCompany());
        v.setLogisticsNo(o.getLogisticsNo());
        v.setStatus(o.getStatus());
        v.setRemark(o.getRemark());
        v.setPaidAt(o.getPaidAt());
        v.setShippedAt(o.getShippedAt());
        v.setCompletedAt(o.getCompletedAt());
        v.setCanceledAt(o.getCanceledAt());
        v.setExpireAt(o.getExpireAt());
        v.setCreatedAt(o.getCreatedAt());
        v.setItems(items.stream().map(OrderConverter::toItemView).collect(Collectors.toList()));
        return v;
    }

    public static AdminOrderView toAdminView(Order o, List<OrderItem> items, String nickname) {
        AdminOrderView v = new AdminOrderView();
        v.setOrderNo(o.getOrderNo());
        v.setUserId(o.getUserId());
        v.setNickname(nickname);
        v.setReceiverName(o.getReceiverName());
        v.setReceiverPhone(o.getReceiverPhone());
        v.setReceiverAddress(o.getReceiverAddress());
        v.setProductAmount(o.getProductAmount());
        v.setCouponDiscount(o.getCouponDiscount());
        v.setShippingFee(o.getShippingFee());
        v.setPayAmount(o.getPayAmount());
        v.setLogisticsCompany(o.getLogisticsCompany());
        v.setLogisticsNo(o.getLogisticsNo());
        v.setStatus(o.getStatus());
        v.setRemark(o.getRemark());
        v.setPaidAt(o.getPaidAt());
        v.setShippedAt(o.getShippedAt());
        v.setCompletedAt(o.getCompletedAt());
        v.setCanceledAt(o.getCanceledAt());
        v.setCreatedAt(o.getCreatedAt());
        v.setItems(items.stream().map(OrderConverter::toItemView).collect(Collectors.toList()));
        return v;
    }

    public static OrderItemView toItemView(OrderItem item) {
        OrderItemView v = new OrderItemView();
        v.setProductId(item.getProductId());
        v.setSkuId(item.getSkuId());
        v.setProductName(item.getProductName());
        v.setSkuName(item.getSkuName());
        v.setProductImage(item.getProductImage());
        v.setUnitPrice(item.getUnitPrice());
        v.setQuantity(item.getQuantity());
        v.setSubtotal(item.getSubtotal());
        return v;
    }
}
