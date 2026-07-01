package com.koala.dto.user;

import com.koala.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 后台用户详情：基础信息 + 下单统计。 */
@Data
public class AdminUserDetailView {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private Integer isValid;
    private LocalDateTime createdAt;

    /** 累计已支付订单数(状态非待付款/已取消)。 */
    private long paidOrderCount;
    /** 累计实付金额(已支付订单 pay_amount 合计)。 */
    private BigDecimal totalPaidAmount;

    public static AdminUserDetailView of(User user) {
        AdminUserDetailView v = new AdminUserDetailView();
        v.setId(user.getId());
        v.setNickname(user.getNickname());
        v.setAvatarUrl(user.getAvatarUrl());
        v.setIsValid(user.getIsValid());
        v.setCreatedAt(user.getCreatedAt());
        return v;
    }
}
