package com.koala.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.koala.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`order`")
public class Order extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
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
    private Integer userDeleted;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime expireAt;
    @Version
    private Integer version;
}
