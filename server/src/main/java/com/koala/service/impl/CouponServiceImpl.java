package com.koala.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.coupon.GrantResultView;
import com.koala.dto.coupon.UserCouponView;
import com.koala.entity.Coupon;
import com.koala.entity.UserCoupon;
import com.koala.mapper.CouponMapper;
import com.koala.mapper.UserCouponMapper;
import com.koala.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户券：自动下发（无手动领取 = 后端自动领），通投全员，每模版每人 1 张。
 */
@Slf4j
@Service
public class CouponServiceImpl implements CouponService {

    private static final int NEAR_EXPIRY_DAYS = 3;
    private static final String LOCK_PREFIX = "lock:coupon:grant:";

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedissonClient redisson;

    public CouponServiceImpl(CouponMapper couponMapper, UserCouponMapper userCouponMapper, RedissonClient redisson) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.redisson = redisson;
    }

    @Override
    public GrantResultView autoGrant(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> candidates = listGrantable(now);

        Set<Long> owned = ownedCouponIds(userId);
        List<UserCouponView> granted = new ArrayList<>();
        for (Coupon coupon : candidates) {
            if (owned.contains(coupon.getId())) {
                continue;
            }
            UserCoupon uc = grantOne(userId, coupon.getId(), now);
            if (uc != null) {
                granted.add(toView(uc, coupon, now));
            }
        }

        GrantResultView result = new GrantResultView();
        result.setGrantedCount(granted.size());
        result.setCoupons(granted);
        return result;
    }

    @Override
    public List<UserCouponView> mine(Long userId, Integer status) {
        List<UserCoupon> rows = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .orderByAsc(UserCoupon::getExpireAt));
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        LocalDateTime now = LocalDateTime.now();
        java.util.Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(
                        rows.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Coupon::getId, c -> c));

        return rows.stream()
                .map(uc -> toView(uc, couponMap.get(uc.getCouponId()), now))
                // 锁定态(3)对用户隐藏，等价占用中；其余按请求 status 过滤
                .filter(v -> v.getStatus() != 3)
                .filter(v -> status == null || v.getStatus().equals(status))
                .sorted(Comparator.comparing(UserCouponView::getStatus)
                        .thenComparing(UserCouponView::getExpireAt))
                .collect(Collectors.toList());
    }

    @Override
    public int expireOverdue() {
        return userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                .set(UserCoupon::getStatus, 2)
                .eq(UserCoupon::getStatus, 0)
                .lt(UserCoupon::getExpireAt, LocalDateTime.now()));
    }

    /** 候选券模板：正常且未发完；固定区间还须在窗内。 */
    private List<Coupon> listGrantable(LocalDateTime now) {
        List<Coupon> all = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getIsValid, 1)
                .apply("issued_count < total_count"));
        return all.stream().filter(c -> {
            if (c.getValidityType() != null && c.getValidityType() == 1) {
                return c.getValidStartAt() != null && c.getValidEndAt() != null
                        && !now.isBefore(c.getValidStartAt()) && !now.isAfter(c.getValidEndAt());
            }
            return true;
        }).collect(Collectors.toList());
    }

    private Set<Long> ownedCouponIds(Long userId) {
        return userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, userId)
                        .select(UserCoupon::getCouponId))
                .stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
    }

    /** 单券下发：用户级分布式锁 + 模板 version 乐观锁条件更新 + 唯一索引兜底。 */
    private UserCoupon grantOne(Long userId, Long couponId, LocalDateTime now) {
        RLock lock = redisson.getLock(LOCK_PREFIX + userId + ":" + couponId);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!locked) {
                return null;
            }
            // 锁内复核：未领过
            Long exists = userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponId, couponId));
            if (exists != null && exists > 0) {
                return null;
            }
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null || coupon.getIsValid() == null || coupon.getIsValid() != 1
                    || coupon.getIssuedCount() >= coupon.getTotalCount()) {
                return null;
            }
            // version 乐观锁 + issued_count<total_count 条件更新，防超发
            int affected = couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                    .setSql("issued_count = issued_count + 1")
                    .setSql("version = version + 1")
                    .eq(Coupon::getId, couponId)
                    .eq(Coupon::getVersion, coupon.getVersion())
                    .apply("issued_count < total_count"));
            if (affected == 0) {
                return null;
            }

            UserCoupon uc = new UserCoupon();
            uc.setUserId(userId);
            uc.setCouponId(couponId);
            uc.setStatus(0);
            uc.setExpireAt(computeExpireAt(coupon, now));
            try {
                userCouponMapper.insert(uc);
            } catch (DuplicateKeyException e) {
                // 唯一索引兜底：已领过则回滚发行计数
                couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                        .setSql("issued_count = issued_count - 1")
                        .eq(Coupon::getId, couponId));
                return null;
            }
            return uc;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private LocalDateTime computeExpireAt(Coupon coupon, LocalDateTime now) {
        if (coupon.getValidityType() != null && coupon.getValidityType() == 1) {
            return coupon.getValidEndAt();
        }
        int days = coupon.getValidDays() != null ? coupon.getValidDays() : 0;
        return now.plusDays(days);
    }

    private UserCouponView toView(UserCoupon uc, Coupon coupon, LocalDateTime now) {
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
        // 懒过期：未使用但已过到期时刻 → 视为已过期(2)
        int status = uc.getStatus();
        if (status == 0 && uc.getExpireAt() != null && uc.getExpireAt().isBefore(now)) {
            status = 2;
        }
        v.setStatus(status);
        v.setNearExpiry(status == 0 && uc.getExpireAt() != null
                && !uc.getExpireAt().isAfter(now.plusDays(NEAR_EXPIRY_DAYS)));
        return v;
    }
}
