package com.koala.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单列表/详情视图。 */
@Data
public class OrderView {

    private String orderNo;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal productAmount;
    private BigDecimal couponDiscount;
    private BigDecimal shippingFee;
    private BigDecimal payAmount;
    private String logisticsCompany;
    private String logisticsNo;
    /** 0=待付款 1=待发货 2=待收货 3=已完成 4=已取消 5=售后中 6=已退款 */
    private Integer status;
    private String remark;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private List<OrderItemView> items;
}
