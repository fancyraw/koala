package com.koala.service;

import cn.hutool.core.util.StrUtil;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.common.result.PageResult;
import com.koala.converter.CouponConverter;
import com.koala.dto.coupon.AdminCouponView;
import com.koala.dto.coupon.CouponSaveRequest;
import com.koala.dto.coupon.GrantDetailView;
import com.koala.entity.Coupon;
import com.koala.entity.User;
import com.koala.entity.UserCoupon;
import com.koala.enums.CouponValidityType;
import com.koala.enums.UserCouponStatus;
import com.koala.enums.ValidFlag;
import com.koala.repository.CouponRepository;
import com.koala.repository.UserCouponRepository;
import com.koala.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AdminCouponService(CouponRepository couponRepository, UserCouponRepository userCouponRepository,
                                  UserRepository userRepository, Clock clock) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** 券模板总量兜底：state 是派生态无法直接翻 SQL，内存过滤合理但需限制表大小防被撑爆。 */
    private static final int LIST_HARD_LIMIT = 5000;

    public PageResult<AdminCouponView> list(String state, long page, long size) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Coupon> raw = couponRepository.findAll();
        if (raw.size() >= LIST_HARD_LIMIT) {
            throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(),
                    "券模板数量已超上限 " + LIST_HARD_LIMIT + "，请清理历史模板后重试");
        }
        List<AdminCouponView> all = raw.stream().map(c -> CouponConverter.toAdminView(c, now)).collect(Collectors.toList());
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

    public AdminCouponView detail(Long id) {
        return CouponConverter.toAdminView(requireCoupon(id), LocalDateTime.now(clock));
    }

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
            entity.setIsValid(ValidFlag.ENABLED.code());
            entity.setVersion(0);
            couponRepository.insert(entity);
        } else {
            if (!ValidFlag.isEnabled(entity.getIsValid())) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "已停发的券不可编辑");
            }
            if (entity.getTotalCount() != null && req.getTotalCount() < entity.getIssuedCount()) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "发行总量不能小于已发行量");
            }
            // 乐观锁：并发下发或修改会让 updateById 影响 0 行，明确抛错让管理员刷新重试。
            if (couponRepository.updateById(entity) == 0) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "券模板已被并发修改，请刷新后重试");
            }
        }
        return entity.getId();
    }

    public void stop(Long id) {
        Coupon c = requireCoupon(id);
        if (ValidFlag.DISABLED.is(c.getIsValid())) {
            return;
        }
        Coupon patch = new Coupon();
        patch.setId(id);
        patch.setIsValid(ValidFlag.DISABLED.code());
        couponRepository.updateById(patch);
    }

    public void delete(Long id) {
        Coupon c = requireCoupon(id);
        if (c.getIssuedCount() != null && c.getIssuedCount() > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "已有下发记录，只能停发不能删除");
        }
        couponRepository.deleteById(id);
    }

    public List<GrantDetailView> grants(Long couponId) {
        requireCoupon(couponId);
        LocalDateTime now = LocalDateTime.now(clock);
        List<UserCoupon> rows = userCouponRepository.findByCoupon(couponId);
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = rows.stream().map(UserCoupon::getUserId).collect(Collectors.toSet());
        Map<Long, String> nickMap = userRepository.findByIds(userIds).stream()
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
            if (UserCouponStatus.UNUSED.is(status) && uc.getExpireAt() != null && uc.getExpireAt().isBefore(now)) {
                status = UserCouponStatus.EXPIRED.code();
            }
            v.setStatus(status);
            return v;
        }).collect(Collectors.toList());
    }

    private void validate(CouponSaveRequest req) {
        if (CouponValidityType.FIXED_RANGE.is(req.getValidityType())) {
            if (req.getValidStartAt() == null || req.getValidEndAt() == null) {
                throw new BizException(ErrorCode.PARAM_MISSING.getCode(), "固定区间需填写起止时间");
            }
            if (!req.getValidEndAt().isAfter(req.getValidStartAt())) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "结束时间须晚于开始时间");
            }
        } else if (CouponValidityType.DAYS_AFTER_GRANT.is(req.getValidityType())) {
            if (req.getValidDays() == null || req.getValidDays() <= 0) {
                throw new BizException(ErrorCode.PARAM_MISSING.getCode(), "领取后N天需填写有效天数");
            }
        } else {
            throw new BizException(ErrorCode.PARAM_MISSING.getCode(), "有效期类型不合法");
        }
    }

    private Coupon requireCoupon(Long id) {
        Coupon c = couponRepository.findById(id);
        if (c == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return c;
    }

}
