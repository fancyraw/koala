package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.OrderItem;
import com.koala.mapper.OrderItemMapper;
import com.koala.repository.OrderItemRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final OrderItemMapper orderItemMapper;

    public OrderItemRepositoryImpl(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public void insert(OrderItem item) {
        orderItemMapper.insert(item);
    }

    @Override
    public List<OrderItem> findByOrderNo(String orderNo) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderNo, orderNo));
    }

    @Override
    public List<OrderItem> findByOrderNos(Collection<String> orderNos) {
        if (orderNos == null || orderNos.isEmpty()) {
            return Collections.emptyList();
        }
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderNo, orderNos));
    }
}
