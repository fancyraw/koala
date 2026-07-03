package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.UserCoupon;
import com.koala.enums.UserCouponStatus;
import com.koala.mapper.UserCouponMapper;
import com.koala.repository.UserCouponRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponMapper userCouponMapper;

    public UserCouponRepositoryImpl(UserCouponMapper userCouponMapper) {
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    public List<UserCoupon> findByUser(Long userId) {
        return userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .orderByAsc(UserCoupon::getExpireAt));
    }

    @Override
    public List<UserCoupon> findByCoupon(Long couponId) {
        return userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getCouponId, couponId)
                .orderByDesc(UserCoupon::getGrantedAt));
    }

    @Override
    public List<UserCoupon> findUsableByUser(Long userId, LocalDateTime now) {
        return userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.code())
                .gt(UserCoupon::getExpireAt, now)
                .orderByAsc(UserCoupon::getExpireAt));
    }

    @Override
    public Set<Long> ownedCouponIds(Long userId) {
        return userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, userId)
                        .select(UserCoupon::getCouponId))
                .stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
    }

    @Override
    public boolean existsByUserAndCoupon(Long userId, Long couponId) {
        Long count = userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        return count != null && count > 0;
    }

    @Override
    public void insert(UserCoupon userCoupon) {
        userCouponMapper.insert(userCoupon);
    }

    @Override
    public int expireOverdue(LocalDateTime now) {
        return userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                .set(UserCoupon::getStatus, UserCouponStatus.EXPIRED.code())
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.code())
                .lt(UserCoupon::getExpireAt, now));
    }

    @Override
    public int lockForOrder(Long userCouponId, String orderNo) {
        return userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                .set(UserCoupon::getStatus, UserCouponStatus.LOCKED.code())
                .set(UserCoupon::getLockOrderNo, orderNo)
                .eq(UserCoupon::getId, userCouponId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.code()));
    }

    @Override
    public int redeem(Long userCouponId, LocalDateTime usedAt) {
        return userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                .set(UserCoupon::getStatus, UserCouponStatus.USED.code())
                .set(UserCoupon::getUsedAt, usedAt)
                .eq(UserCoupon::getId, userCouponId)
                .eq(UserCoupon::getStatus, UserCouponStatus.LOCKED.code()));
    }

    @Override
    public void restoreIfNotExpired(Long userCouponId, LocalDateTime now) {
        userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                .set(UserCoupon::getStatus, UserCouponStatus.UNUSED.code())
                .set(UserCoupon::getUsedAt, null)
                .set(UserCoupon::getLockOrderNo, null)
                .eq(UserCoupon::getId, userCouponId)
                .gt(UserCoupon::getExpireAt, now));
    }

    @Override
    public void releaseLock(Long userCouponId) {
        userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                .set(UserCoupon::getStatus, UserCouponStatus.UNUSED.code())
                .set(UserCoupon::getLockOrderNo, null)
                .eq(UserCoupon::getId, userCouponId)
                .eq(UserCoupon::getStatus, UserCouponStatus.LOCKED.code()));
    }
}
