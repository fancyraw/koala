package com.koala.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付记录：created_at 由 DB 默认值填充。 */
@Data
@TableName("payment")
public class Payment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String channel;
    private String transactionId;
    private BigDecimal payAmount;
    /** 0=待支付 1=成功 2=失败 3=已退款 */
    private Integer status;
    private LocalDateTime paidAt;
    @TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
