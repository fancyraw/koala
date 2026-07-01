package com.koala.infra.pay;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * 微信支付(JSAPI)渠道。开发环境为 mock 实现：prepay 直接返回可用调起参数，
 * parseNotify 解析 mock 回调 JSON(order_no/transaction_id/pay_amount/success)。
 * 生产接入真实微信支付时替换实现即可，OrderService 不受影响。
 */
@Slf4j
@Component
public class WechatPayChannel implements PaymentChannel {

    @Override
    public String code() {
        return "wechat";
    }

    @Override
    public PrepayResult prepay(PrepayCommand cmd) {
        PrepayResult r = new PrepayResult();
        String prepayId = "mock_prepay_" + IdUtil.fastSimpleUUID();
        r.setPrepayId(prepayId);
        r.getPayParams().put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        r.getPayParams().put("nonceStr", IdUtil.fastSimpleUUID());
        r.getPayParams().put("package", "prepay_id=" + prepayId);
        r.getPayParams().put("signType", "RSA");
        r.getPayParams().put("paySign", "mock_sign");
        return r;
    }

    @Override
    public NotifyResult parseNotify(HttpServletRequest request) {
        NotifyResult result = new NotifyResult();
        try {
            String body = IoUtil.read(request.getInputStream(), StandardCharsets.UTF_8);
            JSONObject json = JSONUtil.parseObj(body);
            result.setOrderNo(json.getStr("order_no"));
            result.setTransactionId(json.getStr("transaction_id"));
            if (json.getBigDecimal("pay_amount") != null) {
                result.setPayAmount(json.getBigDecimal("pay_amount"));
            } else {
                result.setPayAmount(BigDecimal.ZERO);
            }
            // mock 验签：默认成功，可传 success=false 模拟失败
            result.setSuccess(json.getBool("success", true));
        } catch (Exception e) {
            log.warn("解析支付回调失败", e);
            result.setSuccess(false);
        }
        return result;
    }

    @Override
    public RefundResult refund(RefundCommand cmd) {
        RefundResult r = new RefundResult();
        r.setSuccess(true);
        r.setRefundId("mock_refund_" + IdUtil.fastSimpleUUID());
        return r;
    }
}
