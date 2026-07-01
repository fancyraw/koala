package com.koala.dto.coupon;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 后台券模板视图：state 为时间+停发派生态。 */
@Data
public class AdminCouponView {

    private Long id;
    private String name;
    private Integer type;
    private BigDecimal discountAmount;
    private BigDecimal minSpend;
    private Integer totalCount;
    private Integer issuedCount;
    private Integer usedCount;
    private Integer validityType;
    private LocalDateTime validStartAt;
    private LocalDateTime validEndAt;
    private Integer validDays;
    /** 派生态：NOT_STARTED/ONGOING/SOLD_OUT/ENDED/STOPPED */
    private String state;
    /** 是否可删（无任何下发记录 issued_count=0） */
    private boolean deletable;
}
