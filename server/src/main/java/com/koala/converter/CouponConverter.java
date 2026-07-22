package com.koala.converter;

import com.koala.dto.coupon.AdminCouponView;
import com.koala.dto.coupon.UserCouponView;
import com.koala.service.pricing.PriceResult;
import com.koala.entity.Coupon;
import com.koala.entity.UserCoupon;
import com.koala.enums.CouponValidityType;
import com.koala.enums.UserCouponStatus;
import com.koala.enums.ValidFlag;

import java.time.LocalDateTime;

/**
 * 优惠券 entity → 视图 + 派生态 / 有效期计算。
 * 纯逻辑无 I/O，方便复用给算价、我的券、后台券管理。
 */
public final class CouponConverter {

    /** 券即将过期提示阈值（天）。 */
    private static final int NEAR_EXPIRY_DAYS = 3;

    private CouponConverter() {}

    /** 用户视角：已过到期时刻的未使用券展示为已过期，另附临近过期标记。 */
    public static UserCouponView toView(UserCoupon uc, Coupon coupon, LocalDateTime now) {
        UserCouponView v = new UserCouponView();
        v.setId(uc.getId());
        v.setCouponId(uc.getCouponId());
        v.setGrantedAt(uc.getGrantedAt());
        v.setUsedAt(uc.getUsedAt());
        v.setExpireAt(uc.getExpireAt());
        if (coupon != null) {
            v.setName(coupon.getName());
            v.setType(coupon.getType());
            v.setDiscountAmount(coupon.getDiscountAmount());
            v.setMinSpend(coupon.getMinSpend());
        }
        int status = uc.getStatus();
        if (UserCouponStatus.UNUSED.is(status) && uc.getExpireAt() != null && uc.getExpireAt().isBefore(now)) {
            status = UserCouponStatus.EXPIRED.code();
        }
        v.setStatus(status);
        v.setNearExpiry(UserCouponStatus.UNUSED.is(status) && uc.getExpireAt() != null
                && !uc.getExpireAt().isAfter(now.plusDays(NEAR_EXPIRY_DAYS)));
        return v;
    }

    /** 后台视角：叠加派生态（进行中/未开始/已结束/售罄/已停发）。 */
    public static AdminCouponView toAdminView(Coupon c, LocalDateTime now) {
        AdminCouponView v = new AdminCouponView();
        v.setId(c.getId());
        v.setName(c.getName());
        v.setType(c.getType());
        v.setDiscountAmount(c.getDiscountAmount());
        v.setMinSpend(c.getMinSpend());
        v.setTotalCount(c.getTotalCount());
        v.setIssuedCount(c.getIssuedCount());
        v.setUsedCount(c.getUsedCount());
        v.setValidityType(c.getValidityType());
        v.setValidStartAt(c.getValidStartAt());
        v.setValidEndAt(c.getValidEndAt());
        v.setValidDays(c.getValidDays());
        v.setState(deriveState(c, now));
        v.setDeletable(c.getIssuedCount() == null || c.getIssuedCount() == 0);
        return v;
    }

    /** 派生态：停发优先级最高；固定区间叠加未开始/已结束。 */
    public static String deriveState(Coupon c, LocalDateTime now) {
        if (!ValidFlag.isEnabled(c.getIsValid())) {
            return "STOPPED";
        }
        boolean soldOut = c.getIssuedCount() != null && c.getTotalCount() != null
                && c.getIssuedCount() >= c.getTotalCount();
        if (CouponValidityType.FIXED_RANGE.is(c.getValidityType())) {
            if (c.getValidStartAt() != null && now.isBefore(c.getValidStartAt())) {
                return "NOT_STARTED";
            }
            if (c.getValidEndAt() != null && now.isAfter(c.getValidEndAt())) {
                return "ENDED";
            }
        }
        return soldOut ? "SOLD_OUT" : "ONGOING";
    }

    /** 有效期计算：固定区间用模板结束时间，领后 N 天用 now + validDays。 */
    public static LocalDateTime computeExpireAt(Coupon coupon, LocalDateTime now) {
        if (CouponValidityType.FIXED_RANGE.is(coupon.getValidityType())) {
            return coupon.getValidEndAt();
        }
        int days = coupon.getValidDays() != null ? coupon.getValidDays() : 0;
        return now.plusDays(days);
    }

    /** 算价用：已应用券的展示项。 */
    public static PriceResult.AppliedCoupon appliedView(UserCoupon uc, Coupon c) {
        PriceResult.AppliedCoupon v = new PriceResult.AppliedCoupon();
        v.setUserCouponId(uc.getId());
        v.setCouponId(c.getId());
        v.setCouponType(c.getType());
        v.setCouponName(c.getName());
        v.setDiscountAmount(c.getDiscountAmount());
        return v;
    }
}
