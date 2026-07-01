package com.koala.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 后台订单视图：含买家昵称。 */
@Data
public class AdminOrderView {

    private String orderNo;
    private Long userId;
    private String nickname;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal productAmount;
    private BigDecimal couponDiscount;
    private BigDecimal shippingFee;
    private BigDecimal payAmount;
    private String logisticsCompany;
    private String logisticsNo;
    private Integer status;
    private String remark;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private List<OrderItemView> items;
}
