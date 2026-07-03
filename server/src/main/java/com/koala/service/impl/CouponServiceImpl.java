package com.koala.service.impl;

import com.koala.dto.coupon.GrantResultView;
import com.koala.dto.coupon.UserCouponView;
import com.koala.entity.Coupon;
import com.koala.entity.UserCoupon;
import com.koala.enums.CouponValidityType;
import com.koala.enums.UserCouponStatus;
import com.koala.enums.ValidFlag;
import com.koala.repository.CouponRepository;
import com.koala.repository.UserCouponRepository;
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

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final RedissonClient redisson;

    public CouponServiceImpl(CouponRepository couponRepository, UserCouponRepository userCouponRepository,
                             RedissonClient redisson) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.redisson = redisson;
    }

    @Override
    public GrantResultView autoGrant(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> candidates = listGrantable(now);

        Set<Long> owned = userCouponRepository.ownedCouponIds(userId);
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
        List<UserCoupon> rows = userCouponRepository.findByUser(userId);
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        LocalDateTime now = LocalDateTime.now();
        java.util.Map<Long, Coupon> couponMap = couponRepository.findByIds(
                        rows.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Coupon::getId, c -> c));

        return rows.stream()
                .map(uc -> toView(uc, couponMap.get(uc.getCouponId()), now))
                // 锁定态(3)对用户隐藏，等价占用中；其余按请求 status 过滤
                .filter(v -> !UserCouponStatus.LOCKED.is(v.getStatus()))
                .filter(v -> status == null || v.getStatus().equals(status))
                .sorted(Comparator.comparing(UserCouponView::getStatus)
                        .thenComparing(UserCouponView::getExpireAt))
                .collect(Collectors.toList());
    }

    @Override
    public int expireOverdue() {
        return userCouponRepository.expireOverdue(LocalDateTime.now());
    }

    /** 候选券模板：正常且未发完；固定区间还须在窗内。 */
    private List<Coupon> listGrantable(LocalDateTime now) {
        return couponRepository.findGrantable().stream().filter(c -> {
            if (CouponValidityType.FIXED_RANGE.is(c.getValidityType())) {
                return c.getValidStartAt() != null && c.getValidEndAt() != null
                        && !now.isBefore(c.getValidStartAt()) && !now.isAfter(c.getValidEndAt());
            }
            return true;
        }).collect(Collectors.toList());
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
            if (userCouponRepository.existsByUserAndCoupon(userId, couponId)) {
                return null;
            }
            Coupon coupon = couponRepository.findById(couponId);
            if (coupon == null || !ValidFlag.isEnabled(coupon.getIsValid())
                    || coupon.getIssuedCount() >= coupon.getTotalCount()) {
                return null;
            }
            // version 乐观锁 + issued_count<total_count 条件更新，防超发
            if (couponRepository.incrementIssuedCas(couponId, coupon.getVersion()) == 0) {
                return null;
            }

            UserCoupon uc = new UserCoupon();
            uc.setUserId(userId);
            uc.setCouponId(couponId);
            uc.setStatus(UserCouponStatus.UNUSED.code());
            uc.setExpireAt(computeExpireAt(coupon, now));
            try {
                userCouponRepository.insert(uc);
            } catch (DuplicateKeyException e) {
                // 唯一索引兜底：已领过则回滚发行计数
                couponRepository.decrementIssued(couponId);
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
        if (CouponValidityType.FIXED_RANGE.is(coupon.getValidityType())) {
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
        if (UserCouponStatus.UNUSED.is(status) && uc.getExpireAt() != null && uc.getExpireAt().isBefore(now)) {
            status = UserCouponStatus.EXPIRED.code();
        }
        v.setStatus(status);
        v.setNearExpiry(UserCouponStatus.UNUSED.is(status) && uc.getExpireAt() != null
                && !uc.getExpireAt().isAfter(now.plusDays(NEAR_EXPIRY_DAYS)));
        return v;
    }
}
