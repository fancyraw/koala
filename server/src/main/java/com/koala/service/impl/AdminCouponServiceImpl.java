package com.koala.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.common.result.PageResult;
import com.koala.dto.coupon.AdminCouponView;
import com.koala.dto.coupon.CouponSaveRequest;
import com.koala.dto.coupon.GrantDetailView;
import com.koala.entity.Coupon;
import com.koala.entity.User;
import com.koala.entity.UserCoupon;
import com.koala.mapper.CouponMapper;
import com.koala.mapper.UserCouponMapper;
import com.koala.mapper.UserMapper;
import com.koala.service.AdminCouponService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminCouponServiceImpl implements AdminCouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserMapper userMapper;

    public AdminCouponServiceImpl(CouponMapper couponMapper, UserCouponMapper userCouponMapper, UserMapper userMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<AdminCouponView> list(String state, long page, long size) {
        LocalDateTime now = LocalDateTime.now();
        List<AdminCouponView> all = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                        .orderByDesc(Coupon::getId))
                .stream().map(c -> toView(c, now)).collect(Collectors.toList());
        if (StrUtil.isNotBlank(state)) {
            all = all.stream().filter(v -> state.equals(v.getState())).collect(Collectors.toList());
        }
        long total = all.size();
        int from = (int) Math.max(0, (page - 1) * size);
        if (from >= all.size()) {
            return new PageResult<>(Collections.emptyList(), total, page, size);
        }
        int to = (int) Math.min(all.size(), from + size);
        return new PageResult<>(all.subList(from, to), total, page, size);
    }

    @Override
    public AdminCouponView detail(Long id) {
        return toView(requireCoupon(id), LocalDateTime.now());
    }

    @Override
    public Long save(CouponSaveRequest req, Long adminId) {
        validate(req);
        Coupon entity = req.getId() != null ? requireCoupon(req.getId()) : new Coupon();
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setDiscountAmount(req.getDiscountAmount());
        entity.setMinSpend(req.getMinSpend() != null ? req.getMinSpend() : BigDecimal.ZERO);
        entity.setTotalCount(req.getTotalCount());
        entity.setValidityType(req.getValidityType());
        entity.setValidStartAt(req.getValidStartAt());
        entity.setValidEndAt(req.getValidEndAt());
        entity.setValidDays(req.getValidDays());
        if (entity.getId() == null) {
            entity.setIssuedCount(0);
            entity.setUsedCount(0);
            entity.setIsValid(1);
            entity.setVersion(0);
            couponMapper.insert(entity);
        } else {
            if (entity.getIsValid() == null || entity.getIsValid() != 1) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "已停发的券不可编辑");
            }
            if (entity.getTotalCount() != null && req.getTotalCount() < entity.getIssuedCount()) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "发行总量不能小于已发行量");
            }
            couponMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void stop(Long id) {
        Coupon c = requireCoupon(id);
        if (c.getIsValid() != null && c.getIsValid() == 0) {
            return;
        }
        Coupon patch = new Coupon();
        patch.setId(id);
        patch.setIsValid(0);
        couponMapper.updateById(patch);
    }

    @Override
    public void delete(Long id) {
        Coupon c = requireCoupon(id);
        if (c.getIssuedCount() != null && c.getIssuedCount() > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "已有下发记录，只能停发不能删除");
        }
        couponMapper.deleteById(id);
    }

    @Override
    public List<GrantDetailView> grants(Long couponId) {
        requireCoupon(couponId);
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> rows = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getCouponId, couponId)
                .orderByDesc(UserCoupon::getGrantedAt));
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = rows.stream().map(UserCoupon::getUserId).collect(Collectors.toSet());
        Map<Long, String> nickMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return rows.stream().map(uc -> {
            GrantDetailView v = new GrantDetailView();
            v.setUserCouponId(uc.getId());
            v.setUserId(uc.getUserId());
            v.setNickname(nickMap.get(uc.getUserId()));
            v.setGrantedAt(uc.getGrantedAt());
            v.setUsedAt(uc.getUsedAt());
            v.setExpireAt(uc.getExpireAt());
            int status = uc.getStatus();
            if (status == 0 && uc.getExpireAt() != null && uc.getExpireAt().isBefore(now)) {
                status = 2;
            }
            v.setStatus(status);
            return v;
        }).collect(Collectors.toList());
    }

    private void validate(CouponSaveRequest req) {
        if (req.getValidityType() != null && req.getValidityType() == 1) {
            if (req.getValidStartAt() == null || req.getValidEndAt() == null) {
                throw new BizException(ErrorCode.PARAM_MISSING.getCode(), "固定区间需填写起止时间");
            }
            if (!req.getValidEndAt().isAfter(req.getValidStartAt())) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "结束时间须晚于开始时间");
            }
        } else if (req.getValidityType() != null && req.getValidityType() == 2) {
            if (req.getValidDays() == null || req.getValidDays() <= 0) {
                throw new BizException(ErrorCode.PARAM_MISSING.getCode(), "领取后N天需填写有效天数");
            }
        } else {
            throw new BizException(ErrorCode.PARAM_MISSING.getCode(), "有效期类型不合法");
        }
    }

    private Coupon requireCoupon(Long id) {
        Coupon c = couponMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return c;
    }

    private AdminCouponView toView(Coupon c, LocalDateTime now) {
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
    private String deriveState(Coupon c, LocalDateTime now) {
        if (c.getIsValid() == null || c.getIsValid() == 0) {
            return "STOPPED";
        }
        boolean soldOut = c.getIssuedCount() != null && c.getTotalCount() != null
                && c.getIssuedCount() >= c.getTotalCount();
        if (c.getValidityType() != null && c.getValidityType() == 1) {
            if (c.getValidStartAt() != null && now.isBefore(c.getValidStartAt())) {
                return "NOT_STARTED";
            }
            if (c.getValidEndAt() != null && now.isAfter(c.getValidEndAt())) {
                return "ENDED";
            }
        }
        return soldOut ? "SOLD_OUT" : "ONGOING";
    }
}
