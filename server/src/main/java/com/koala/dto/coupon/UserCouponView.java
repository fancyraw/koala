package com.koala.dto.coupon;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 我的券：含模板规则冗余 + 派生临期标记。 */
@Data
public class UserCouponView {

    private Long id;
    private Long couponId;
    private String name;
    /** 1=满减券 2=无门槛券 */
    private Integer type;
    private BigDecimal discountAmount;
    private BigDecimal minSpend;
    /** 0=未使用 1=已使用 2=已过期 3=已锁定 */
    private Integer status;
    private LocalDateTime expireAt;
    private LocalDateTime grantedAt;
    private LocalDateTime usedAt;
    /** 派生：未使用且距到期 <=3 天 */
    private boolean nearExpiry;
}
