package com.koala.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.koala.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon")
public class Coupon extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 1=满减券 2=无门槛券 */
    private Integer type;
    private BigDecimal discountAmount;
    /** 满减门槛(无门槛=0) */
    private BigDecimal minSpend;
    private Integer totalCount;
    private Integer issuedCount;
    private Integer usedCount;
    /** 1=固定区间 2=领取后N天 */
    private Integer validityType;
    private LocalDateTime validStartAt;
    private LocalDateTime validEndAt;
    private Integer validDays;
    /** 1=正常 0=已停发(终态) */
    private Integer isValid;
    @Version
    private Integer version;
}
