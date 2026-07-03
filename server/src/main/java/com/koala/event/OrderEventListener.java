package com.koala.event;

import com.koala.entity.OrderItem;
import com.koala.repository.OrderItemRepository;
import com.koala.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/** 订单副作用监听：支付成功销量+1，退款按策略回滚销量。新增副作用只加监听器，不动主流程。 */
@Slf4j
@Component
public class OrderEventListener {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderEventListener(OrderItemRepository orderItemRepository, ProductRepository productRepository) {
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        adjustSales(event.getOrderNo(), 1);
        log.info("[OrderPaid] 销量已累加 orderNo={}", event.getOrderNo());
    }

    @EventListener
    public void onRefunded(RefundedEvent event) {
        adjustSales(event.getOrderNo(), -1);
        log.info("[Refunded] 销量已回滚 orderNo={}", event.getOrderNo());
    }

    private void adjustSales(String orderNo, int sign) {
        List<OrderItem> items = orderItemRepository.findByOrderNo(orderNo);
        for (OrderItem item : items) {
            productRepository.addSalesCount(item.getProductId(), sign * item.getQuantity());
        }
    }
}
