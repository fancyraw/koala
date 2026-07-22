package com.koala.repository;

import com.koala.entity.Coupon;

import java.util.Collection;
import java.util.List;

public interface CouponRepository {

    Coupon findById(Long id);

    List<Coupon> findByIds(Collection<Long> ids);

    /** 全部券模板,按 id 倒序(后台列表)。 */
    List<Coupon> findAll();

    /** 可下发候选:启用且 issued_count < total_count(窗口由调用方再筛)。 */
    List<Coupon> findGrantable();

    void insert(Coupon coupon);

    /** 更新，返回受影响行数（带 @Version 时并发失败为 0）。 */
    int updateById(Coupon coupon);

    void deleteById(Long id);

    /**
     * 发行计数 +1(乐观锁):version 命中且 issued_count < total_count 才成功。
     * 同时 version+1。返回受影响行数。
     */
    int incrementIssuedCas(Long couponId, int expectVersion);

    /** 发行计数 -1(唯一索引兜底回滚)。 */
    void decrementIssued(Long couponId);

    /** 核销计数 +1。 */
    void incrementUsedCount(Long couponId);
}
