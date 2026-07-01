package com.koala.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 用户券：granted_at 由 DB 默认值填充，故不继承 BaseEntity。 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long couponId;
    /** 0=未使用 1=已使用 2=已过期 3=已锁定(下单未支付) */
    private Integer status;
    private String lockOrderNo;
    private LocalDateTime expireAt;
    @TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime grantedAt;
    private LocalDateTime usedAt;
}
