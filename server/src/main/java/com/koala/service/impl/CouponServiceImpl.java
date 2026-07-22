package com.koala.service.impl;

import com.koala.common.constant.RedisKeys;
import com.koala.converter.CouponConverter;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final RedissonClient redisson;

    /** 自注入代理：私有事务方法需走代理，否则 @Transactional 失效。 */
    @Autowired
    @Lazy
    private CouponServiceImpl self;

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
                granted.add(CouponConverter.toView(uc, coupon, now));
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
                .map(uc -> CouponConverter.toView(uc, couponMap.get(uc.getCouponId()), now))
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

    /** 单券下发：用户级分布式锁 + 事务包裹 CAS+插入，保证发行计数与用户券行原子提交。 */
    private UserCoupon grantOne(Long userId, Long couponId, LocalDateTime now) {
        RLock lock = redisson.getLock(RedisKeys.LOCK_COUPON_GRANT + userId + ":" + couponId);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!locked) {
                return null;
            }
            try {
                return self.tryGrantOne(userId, couponId, now);
            } catch (DuplicateKeyException e) {
                // 事务已回滚，issued_count 未变化，视作幂等跳过。
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 事务化 CAS+插入：任一步失败整体回滚，避免 issued_count 与 user_coupon 之间发生半提交。
     * DuplicateKeyException 走上层 catch 视为幂等 —— 事务此时已回滚，issued_count 不会漂移。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon tryGrantOne(Long userId, Long couponId, LocalDateTime now) {
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
        uc.setExpireAt(CouponConverter.computeExpireAt(coupon, now));
        // 唯一索引兜底：并发下发时 insert 抛 DuplicateKeyException，
        // Spring 视其为需回滚的 DataAccessException，`incrementIssuedCas` 会随事务撤销，
        // 无需手动 decrementIssued。异常由调用者 catch 后视作幂等。
        userCouponRepository.insert(uc);
        return uc;
    }

}
