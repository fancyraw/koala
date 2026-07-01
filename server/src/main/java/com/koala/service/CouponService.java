package com.koala.service;

import com.koala.dto.coupon.GrantResultView;
import com.koala.dto.coupon.UserCouponView;

import java.util.List;

public interface CouponService {

    /** 自动下发：把所有可下发且用户未领过的券各发1张，返回本次新到账券。幂等。 */
    GrantResultView autoGrant(Long userId);

    /** 我的券：status 可空(全部)或 0/1/2 过滤；未使用按到期升序、含临期标记。 */
    List<UserCouponView> mine(Long userId, Integer status);

    /** 过期巡检(每天,7.4)：未使用(0)且已到期 → 2已过期。返回处理条数。 */
    int expireOverdue();
}
