package com.koala.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 算价结果(6.2)：预览与下单共用。含选中的最优券组合。 */
@Data
public class PriceResult {

    private BigDecimal productAmount = BigDecimal.ZERO;
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal payAmount = BigDecimal.ZERO;

    /** 本次选中抵扣的用户券(无门槛/满减各≤1张)。 */
    private List<AppliedCoupon> appliedCoupons = new ArrayList<>();

    @Data
    public static class AppliedCoupon {
        private Long userCouponId;
        private Long couponId;
        /** 1=满减 2=无门槛 */
        private Integer couponType;
        private String couponName;
        private BigDecimal discountAmount;
    }
}
