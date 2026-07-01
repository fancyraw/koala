package com.koala.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.OrderItem;
import com.koala.entity.Product;
import com.koala.mapper.OrderItemMapper;
import com.koala.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/** 订单副作用监听：支付成功销量+1，退款按策略回滚销量。新增副作用只加监听器，不动主流程。 */
@Slf4j
@Component
public class OrderEventListener {

    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    public OrderEventListener(OrderItemMapper orderItemMapper, ProductMapper productMapper) {
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
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
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderNo, orderNo));
        for (OrderItem item : items) {
            int delta = sign * item.getQuantity();
            productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                    .setSql("sales_count = GREATEST(0, sales_count + (" + delta + "))")
                    .eq(Product::getId, item.getProductId()));
        }
    }
}
