package com.koala.dto.aftersale;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** C端售后单视图(售后进度页)。 */
@Data
public class AfterSaleView {

    private String afterSaleNo;
    private String orderNo;
    private Integer type;
    private String reason;
    private String remark;
    private List<String> evidenceImages;
    private BigDecimal refundAmount;
    private String returnTrackingNo;
    /** 0待审核 1通过待寄回 2买家已寄回 3商家已收货 4已退款 5已拒绝 */
    private Integer status;
    private String auditRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
