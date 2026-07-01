package com.koala.dto.coupon;

import lombok.Data;

import java.time.LocalDateTime;

/** 券发放明细行：该券模板下发的某张用户券。 */
@Data
public class GrantDetailView {

    private Long userCouponId;
    private Long userId;
    private String nickname;
    /** 0=未使用 1=已使用 2=已过期 3=已锁定 */
    private Integer status;
    private LocalDateTime grantedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expireAt;
}
