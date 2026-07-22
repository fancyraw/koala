package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.Coupon;
import com.koala.enums.ValidFlag;
import com.koala.mapper.CouponMapper;
import com.koala.repository.CouponRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponMapper couponMapper;

    public CouponRepositoryImpl(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Override
    public Coupon findById(Long id) {
        return couponMapper.selectById(id);
    }

    @Override
    public List<Coupon> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return couponMapper.selectBatchIds(ids);
    }

    @Override
    public List<Coupon> findAll() {
        return couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .orderByDesc(Coupon::getId));
    }

    @Override
    public List<Coupon> findGrantable() {
        return couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getIsValid, ValidFlag.ENABLED.code())
                .apply("issued_count < total_count"));
    }

    @Override
    public void insert(Coupon coupon) {
        couponMapper.insert(coupon);
    }

    @Override
    public int updateById(Coupon coupon) {
        return couponMapper.updateById(coupon);
    }

    @Override
    public void deleteById(Long id) {
        couponMapper.deleteById(id);
    }

    @Override
    public int incrementIssuedCas(Long couponId, int expectVersion) {
        return couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .setSql("issued_count = issued_count + 1")
                .setSql("version = version + 1")
                .eq(Coupon::getId, couponId)
                .eq(Coupon::getVersion, expectVersion)
                .apply("issued_count < total_count"));
    }

    @Override
    public void decrementIssued(Long couponId) {
        couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .setSql("issued_count = issued_count - 1")
                .eq(Coupon::getId, couponId));
    }

    @Override
    public void incrementUsedCount(Long couponId) {
        couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .setSql("used_count = used_count + 1")
                .eq(Coupon::getId, couponId));
    }
}
