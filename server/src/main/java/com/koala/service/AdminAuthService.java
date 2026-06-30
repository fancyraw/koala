package com.koala.service;

import com.koala.dto.auth.QrcodeCheckResponse;
import com.koala.dto.auth.QrcodeResponse;

public interface AdminAuthService {

    /** 生成扫码登录会话 + 二维码内容。 */
    QrcodeResponse createQrcode();

    /** 前端轮询扫码结果。 */
    QrcodeCheckResponse check(String state);

    /** 微信扫码回调：code 换 openid，匹配启用管理员，结果绑定到 state。 */
    void handleCallback(String state, String code);
}
