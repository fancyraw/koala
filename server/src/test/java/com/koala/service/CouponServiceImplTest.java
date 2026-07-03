package com.koala.service;

import com.koala.dto.coupon.GrantResultView;
import com.koala.entity.Coupon;
import com.koala.entity.UserCoupon;
import com.koala.enums.CouponType;
import com.koala.enums.CouponValidityType;
import com.koala.enums.UserCouponStatus;
import com.koala.enums.ValidFlag;
import com.koala.repository.CouponRepository;
import com.koala.repository.UserCouponRepository;
import com.koala.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private RedissonClient redisson;
    @Mock
    private RLock lock;

    @InjectMocks
    private CouponServiceImpl service;

    @BeforeEach
    void setUpLock() throws InterruptedException {
        lenient().when(redisson.getLock(anyString())).thenReturn(lock);
        lenient().when(lock.tryLock(anyLong(), anyLong(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        lenient().when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    private Coupon noThreshold(long id, String amount, int total, int issued) {
        Coupon c = new Coupon();
        c.setId(id);
        c.setName("无门槛券");
        c.setType(CouponType.NO_THRESHOLD.code());
        c.setDiscountAmount(new BigDecimal(amount));
        c.setTotalCount(total);
        c.setIssuedCount(issued);
        c.setValidityType(CouponValidityType.DAYS_AFTER_GRANT.code());
        c.setValidDays(7);
        c.setIsValid(ValidFlag.ENABLED.code());
        c.setVersion(0);
        return c;
    }

    @Test
    void autoGrant_newCoupon_grantsAndInserts() {
        Coupon c = noThreshold(1L, "5", 100, 10);
        when(couponRepository.findGrantable()).thenReturn(Collections.singletonList(c));
        when(userCouponRepository.ownedCouponIds(1L)).thenReturn(Collections.emptySet());
        when(userCouponRepository.existsByUserAndCoupon(1L, 1L)).thenReturn(false);
        when(couponRepository.findById(1L)).thenReturn(c);
        when(couponRepository.incrementIssuedCas(1L, 0)).thenReturn(1);

        GrantResultView result = service.autoGrant(1L);

        assertThat(result.getGrantedCount()).isEqualTo(1);
        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponRepository).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserCouponStatus.UNUSED.code());
        verify(lock).unlock();
    }

    @Test
    void autoGrant_alreadyOwned_skipsBeforeLock() {
        Coupon c = noThreshold(1L, "5", 100, 10);
        when(couponRepository.findGrantable()).thenReturn(Collections.singletonList(c));
        when(userCouponRepository.ownedCouponIds(1L)).thenReturn(new HashSet<>(Collections.singletonList(1L)));

        GrantResultView result = service.autoGrant(1L);

        assertThat(result.getGrantedCount()).isZero();
        verify(userCouponRepository, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(redisson, never()).getLock(anyString());
    }

    @Test
    void autoGrant_soldOut_skips() {
        Coupon c = noThreshold(1L, "5", 100, 100); // issued == total
        when(couponRepository.findGrantable()).thenReturn(Collections.singletonList(c));
        when(userCouponRepository.ownedCouponIds(1L)).thenReturn(Collections.emptySet());
        when(userCouponRepository.existsByUserAndCoupon(1L, 1L)).thenReturn(false);
        when(couponRepository.findById(1L)).thenReturn(c);

        GrantResultView result = service.autoGrant(1L);

        assertThat(result.getGrantedCount()).isZero();
        verify(couponRepository, never()).incrementIssuedCas(anyLong(), anyInt());
    }

    @Test
    void autoGrant_casLost_doesNotInsert() {
        Coupon c = noThreshold(1L, "5", 100, 10);
        when(couponRepository.findGrantable()).thenReturn(Collections.singletonList(c));
        when(userCouponRepository.ownedCouponIds(1L)).thenReturn(Collections.emptySet());
        when(userCouponRepository.existsByUserAndCoupon(1L, 1L)).thenReturn(false);
        when(couponRepository.findById(1L)).thenReturn(c);
        when(couponRepository.incrementIssuedCas(1L, 0)).thenReturn(0); // 并发抢发失败

        GrantResultView result = service.autoGrant(1L);

        assertThat(result.getGrantedCount()).isZero();
        verify(userCouponRepository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void autoGrant_fixedRangeOutOfWindow_filteredOut() {
        Coupon c = noThreshold(1L, "5", 100, 10);
        c.setValidityType(CouponValidityType.FIXED_RANGE.code());
        c.setValidStartAt(LocalDateTime.now().plusDays(1)); // 尚未开始
        c.setValidEndAt(LocalDateTime.now().plusDays(10));
        when(couponRepository.findGrantable()).thenReturn(Collections.singletonList(c));
        when(userCouponRepository.ownedCouponIds(1L)).thenReturn(Collections.emptySet());

        GrantResultView result = service.autoGrant(1L);

        assertThat(result.getGrantedCount()).isZero();
        verify(userCouponRepository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mine_hidesLockedAndLazyExpires() {
        UserCoupon unused = new UserCoupon();
        unused.setId(1L);
        unused.setCouponId(1L);
        unused.setStatus(UserCouponStatus.UNUSED.code());
        unused.setExpireAt(LocalDateTime.now().minusDays(1)); // 已过期 → 懒过期

        UserCoupon locked = new UserCoupon();
        locked.setId(2L);
        locked.setCouponId(1L);
        locked.setStatus(UserCouponStatus.LOCKED.code());
        locked.setExpireAt(LocalDateTime.now().plusDays(5));

        when(userCouponRepository.findByUser(1L)).thenReturn(Arrays.asList(unused, locked));
        when(couponRepository.findByIds(org.mockito.ArgumentMatchers.anySet()))
                .thenReturn(Collections.singletonList(noThreshold(1L, "5", 100, 10)));

        // 不加 status 过滤：锁定态隐藏，过期项状态被改写为 EXPIRED
        assertThat(service.mine(1L, null))
                .hasSize(1)
                .allSatisfy(v -> assertThat(v.getStatus()).isEqualTo(UserCouponStatus.EXPIRED.code()));
    }

    @Test
    void expireOverdue_delegates() {
        when(userCouponRepository.expireOverdue(org.mockito.ArgumentMatchers.any())).thenReturn(4);
        assertThat(service.expireOverdue()).isEqualTo(4);
    }
}
