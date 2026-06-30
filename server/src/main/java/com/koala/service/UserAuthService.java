package com.koala.service;

import com.koala.dto.auth.LoginResponse;

public interface UserAuthService {

    /** C 端微信登录：code 换 openid，命中即登录，未命中自动注册。 */
    LoginResponse loginByWechat(String code);
}
