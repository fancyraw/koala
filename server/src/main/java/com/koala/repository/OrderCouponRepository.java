package com.koala.repository;

import com.koala.entity.OrderCoupon;

import java.util.List;

public interface OrderCouponRepository {

    void insert(OrderCoupon orderCoupon);

    List<OrderCoupon> findByOrderNo(String orderNo);
}
