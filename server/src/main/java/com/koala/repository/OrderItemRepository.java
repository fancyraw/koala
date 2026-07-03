package com.koala.repository;

import com.koala.entity.OrderItem;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository {

    void insert(OrderItem item);

    List<OrderItem> findByOrderNo(String orderNo);

    List<OrderItem> findByOrderNos(Collection<String> orderNos);
}
