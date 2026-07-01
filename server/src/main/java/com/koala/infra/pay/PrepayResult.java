package com.koala.infra.pay;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** 前端调起支付所需参数(wx.requestPayment)。 */
@Data
public class PrepayResult {

    private String prepayId;
    /** 前端 wx.requestPayment 所需字段(timeStamp/nonceStr/package/signType/paySign)。 */
    private Map<String, String> payParams = new LinkedHashMap<>();
}
