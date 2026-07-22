package com.koala.dto.aftersale;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/** C端申请售后：类型由订单状态自动判断(待发货→仅退款/待收货→退货退款)。退款金额取订单实付。 */
@Data
public class AfterSaleApplyRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "退款原因不能为空")
    private String reason;

    @Size(max = 500, message = "备注最多 500 字")
    private String remark;

    /** 凭证图URL，最多3张，需为 http(s) URL。 */
    @Size(max = 3, message = "凭证图最多3张")
    private List<@Pattern(regexp = "^https?://[\\w\\-./%?=&:#@+,~]{1,500}$",
            message = "凭证图必须为合法的 http(s) URL") String> evidenceImages;
}
