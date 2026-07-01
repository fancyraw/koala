package com.koala.dto.aftersale;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/** C端申请售后：类型由订单状态自动判断(待发货→仅退款/待收货→退货退款)。退款金额取订单实付。 */
@Data
public class AfterSaleApplyRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "退款原因不能为空")
    private String reason;

    private String remark;

    /** 凭证图URL，最多3张 */
    private List<String> evidenceImages;
}
