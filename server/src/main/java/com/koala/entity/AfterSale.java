package com.koala.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.koala.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 售后单：一张售后单独立于订单状态流转。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sale")
public class AfterSale extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String afterSaleNo;
    private String orderNo;
    private Long userId;
    /** 1=仅退款(未发货) 2=退货退款(已发货) */
    private Integer type;
    private String reason;
    private String remark;
    /** 凭证图JSON(最多3张) */
    private String evidenceImages;
    private BigDecimal refundAmount;
    private String returnTrackingNo;
    /** 0待审核 1通过待寄回 2买家已寄回 3商家已收货 4已退款 5已拒绝 */
    private Integer status;
    private String auditRemark;
}
