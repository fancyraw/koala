package com.koala.service;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.order.OrderShipRequest;
import com.koala.entity.Order;
import com.koala.enums.OrderStatus;
import com.koala.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 订单发货：仅待发货订单可置为待收货。 */
@Service
public class OrderShipService {

    private final OrderRepository orderRepository;

    public OrderShipService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void ship(OrderShipRequest req) {
        Order order = orderRepository.findByNo(req.getNo());
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!OrderStatus.WAIT_SHIP.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅待发货订单可发货");
        }
        if (orderRepository.markShippedCas(req.getNo(), req.getLogisticsCompany(),
                req.getLogisticsNo(), LocalDateTime.now()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR);
        }
    }
}
