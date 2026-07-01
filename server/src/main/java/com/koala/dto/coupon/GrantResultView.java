package com.koala.dto.coupon;

import lombok.Data;

import java.util.List;

/** 自动下发结果：本次新到账券，供前端「领券弹窗」。 */
@Data
public class GrantResultView {

    private int grantedCount;
    private List<UserCouponView> coupons;
}
