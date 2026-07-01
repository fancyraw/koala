package com.koala.dto.aftersale;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 后台售后单视图：含买家昵称。 */
@Data
public class AdminAfterSaleView {

    private String afterSaleNo;
    private String orderNo;
    private Long userId;
    private String nickname;
    private Integer type;
    private String reason;
    private String remark;
    private List<String> evidenceImages;
    private BigDecimal refundAmount;
    private String returnTrackingNo;
    private Integer status;
    private String auditRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
