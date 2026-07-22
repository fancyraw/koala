package com.koala.infra.pay;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.koala.common.constant.PaymentChannels;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.config.WechatProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * 微信支付(JSAPI)渠道。
 *
 * <p>当前实现仅提供 mock 路径，供本地/dev 联调；不接入真实 SDK。
 * 真实接入 TODO：引入 {@code com.github.wechatpay-apiv3:wechatpay-java}，
 * 用 {@code JsapiServiceExtension} 做 prepay、{@code NotificationParser} 解密+验签、
 * {@code RefundService} 做退款。所需商户配置（mchid、serialNo、apiV3Key、
 * 私钥路径、notifyUrl）应通过 {@code koala.pay.wechat.*} 注入。
 *
 * <p>安全底线：仅当 {@link WechatProperties#isMockWhenUnconfigured()} 为 true 时才允许 mock；
 * prod 默认关闭，所有方法直接抛异常，防止 pay-notify 被无签名利用。
 */
@Slf4j
@Component
public class WechatPayChannel implements PaymentChannel {

    private final WechatProperties props;

    public WechatPayChannel(WechatProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return PaymentChannels.WECHAT;
    }

    @Override
    public PrepayResult prepay(PrepayCommand cmd) {
        requireMockOrThrow("prepay");
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
        if (!props.isMockWhenUnconfigured()) {
            // prod 未接入真实验签逻辑前，一律拒绝——阻断「无签名任意置支付成功」的攻击面。
            log.warn("拒绝支付回调：微信支付未接入，mockWhenUnconfigured=false");
            result.setSuccess(false);
            return result;
        }
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
        requireMockOrThrow("refund");
        RefundResult r = new RefundResult();
        r.setSuccess(true);
        r.setRefundId("mock_refund_" + IdUtil.fastSimpleUUID());
        return r;
    }

    private void requireMockOrThrow(String op) {
        if (!props.isMockWhenUnconfigured()) {
            log.warn("微信支付未接入，操作被拒绝: {}", op);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信支付未接入，请先配置商户信息");
        }
    }
}
