package com.koala.repository;

import com.koala.entity.UserCoupon;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface UserCouponRepository {

    /** 用户全部券,按 expireAt 升序。 */
    List<UserCoupon> findByUser(Long userId);

    /** 某模板的全部下发记录,按 grantedAt 倒序。 */
    List<UserCoupon> findByCoupon(Long couponId);

    /** 用户可用券:未使用(0)且未过期,按 expireAt 升序。 */
    List<UserCoupon> findUsableByUser(Long userId, LocalDateTime now);

    /** 用户已拥有的模板 id 集合(判重用)。 */
    Set<Long> ownedCouponIds(Long userId);

    boolean existsByUserAndCoupon(Long userId, Long couponId);

    void insert(UserCoupon userCoupon);

    /** 批量把未使用(0)且已过期的券置为已过期(2)。返回受影响行数。 */
    int expireOverdue(LocalDateTime now);

    /** 下单锁定:未使用(0)→锁定(3) 并写 lockOrderNo。返回受影响行数。 */
    int lockForOrder(Long userCouponId, String orderNo);

    /** 支付核销:锁定(3)→已使用(1) 并写 usedAt。返回受影响行数。 */
    int redeem(Long userCouponId, LocalDateTime usedAt);

    /** 退款原路退回:未过期则重置为未使用(0),清空 usedAt/lockOrderNo。 */
    void restoreIfNotExpired(Long userCouponId, LocalDateTime now);

    /** 释放锁定:锁定(3)→未使用(0) 并清空 lockOrderNo。 */
    void releaseLock(Long userCouponId);
}
