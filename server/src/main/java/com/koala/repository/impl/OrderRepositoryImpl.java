package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koala.entity.Order;
import com.koala.enums.OrderStatus;
import com.koala.enums.ValidFlag;
import com.koala.mapper.OrderMapper;
import com.koala.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;

    public OrderRepositoryImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Order findByNo(String orderNo) {
        return orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo));
    }

    @Override
    public void insert(Order order) {
        orderMapper.insert(order);
    }

    @Override
    public IPage<Order> pageByUser(Long userId, Integer status, long page, long size) {
        return orderMapper.selectPage(new Page<>(page, size),
                Wrappers.<Order>lambdaQuery()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getUserDeleted, ValidFlag.DISABLED.code())
                        .eq(status != null, Order::getStatus, status)
                        .orderByDesc(Order::getId));
    }

    @Override
    public IPage<Order> pageForAdmin(Integer status, String keyword, Collection<Long> userIds,
                                     long page, long size) {
        final boolean hasKeyword = keyword != null && !keyword.isEmpty();
        final Collection<Long> uids = userIds;
        return orderMapper.selectPage(new Page<>(page, size),
                Wrappers.<Order>lambdaQuery()
                        .eq(status != null, Order::getStatus, status)
                        .and(hasKeyword, w -> {
                            w.like(Order::getOrderNo, keyword).or().like(Order::getReceiverName, keyword);
                            if (uids != null && !uids.isEmpty()) {
                                w.or().in(Order::getUserId, uids);
                            }
                        })
                        .orderByDesc(Order::getId));
    }

    @Override
    public List<Order> findTimedOutUnpaid(LocalDateTime now) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, OrderStatus.WAIT_PAY.code())
                .lt(Order::getExpireAt, now));
    }

    @Override
    public List<Order> findAutoConfirmDue(LocalDateTime deadline) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, OrderStatus.WAIT_RECEIVE.code())
                .isNotNull(Order::getShippedAt)
                .lt(Order::getShippedAt, deadline));
    }

    @Override
    public List<Order> findPaidByUser(Long userId) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .isNotNull(Order::getPaidAt));
    }

    @Override
    public List<Order> findPaidSince(LocalDateTime since) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .isNotNull(Order::getPaidAt)
                .ge(Order::getPaidAt, since));
    }

    @Override
    public long countByStatus(int status) {
        Long c = orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, status));
        return c == null ? 0 : c;
    }

    @Override
    public int updateStatusCas(String orderNo, int expectStatus, int newStatus) {
        return orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, newStatus)
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, expectStatus));
    }

    @Override
    public int markPaidCas(String orderNo, LocalDateTime paidAt) {
        return orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, OrderStatus.WAIT_SHIP.code())
                .set(Order::getPaidAt, paidAt)
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.WAIT_PAY.code()));
    }

    @Override
    public int markCompletedCas(String orderNo, LocalDateTime completedAt) {
        return orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, OrderStatus.COMPLETED.code())
                .set(Order::getCompletedAt, completedAt)
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.WAIT_RECEIVE.code()));
    }

    @Override
    public int markShippedCas(String orderNo, String logisticsCompany, String logisticsNo, LocalDateTime shippedAt) {
        return orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, OrderStatus.WAIT_RECEIVE.code())
                .set(Order::getLogisticsCompany, logisticsCompany)
                .set(Order::getLogisticsNo, logisticsNo)
                .set(Order::getShippedAt, shippedAt)
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.WAIT_SHIP.code()));
    }

    @Override
    public int cancelFromUnpaidCas(String orderNo, int targetStatus, LocalDateTime canceledAt) {
        return orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, targetStatus)
                .set(Order::getCanceledAt, canceledAt)
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, OrderStatus.WAIT_PAY.code()));
    }

    @Override
    public void markUserDeleted(String orderNo) {
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getUserDeleted, ValidFlag.ENABLED.code())
                .eq(Order::getOrderNo, orderNo));
    }
}
