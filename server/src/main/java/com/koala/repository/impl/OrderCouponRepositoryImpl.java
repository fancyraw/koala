package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.OrderCoupon;
import com.koala.mapper.OrderCouponMapper;
import com.koala.repository.OrderCouponRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderCouponRepositoryImpl implements OrderCouponRepository {

    private final OrderCouponMapper orderCouponMapper;

    public OrderCouponRepositoryImpl(OrderCouponMapper orderCouponMapper) {
        this.orderCouponMapper = orderCouponMapper;
    }

    @Override
    public void insert(OrderCoupon orderCoupon) {
        orderCouponMapper.insert(orderCoupon);
    }

    @Override
    public List<OrderCoupon> findByOrderNo(String orderNo) {
        return orderCouponMapper.selectList(Wrappers.<OrderCoupon>lambdaQuery()
                .eq(OrderCoupon::getOrderNo, orderNo));
    }
}
