package com.koala.infra.pay;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 按 channelCode 路由到具体渠道实现。 */
@Component
public class PaymentChannelFactory {

    private final Map<String, PaymentChannel> channels;

    public PaymentChannelFactory(List<PaymentChannel> channelList) {
        this.channels = channelList.stream()
                .collect(Collectors.toMap(PaymentChannel::code, Function.identity()));
    }

    public PaymentChannel get(String code) {
        PaymentChannel c = channels.get(code);
        if (c == null) {
            throw new BizException(ErrorCode.PAY_FAILED.getCode(), "不支持的支付渠道: " + code);
        }
        return c;
    }
}
