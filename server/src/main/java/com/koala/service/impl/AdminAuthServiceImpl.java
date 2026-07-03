package com.koala.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.koala.common.auth.JwtUtil;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.config.WechatProperties;
import com.koala.dto.auth.LoginResponse;
import com.koala.dto.auth.QrcodeCheckResponse;
import com.koala.dto.auth.QrcodeResponse;
import com.koala.entity.Admin;
import com.koala.enums.ValidFlag;
import com.koala.infra.wechat.WechatAuthClient;
import com.koala.repository.AdminRepository;
import com.koala.service.AdminAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 后台扫码登录：state 会话存 Redis，回调写结果，前端轮询取 token。
 */
@Slf4j
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String SESSION_KEY = "admin:login:session:";
    private static final long TTL_SECONDS = 300;

    private final StringRedisTemplate redis;
    private final WechatAuthClient wechatAuthClient;
    private final WechatProperties wechatProps;
    private final AdminRepository adminRepository;
    private final JwtUtil jwtUtil;

    public AdminAuthServiceImpl(StringRedisTemplate redis, WechatAuthClient wechatAuthClient,
                                WechatProperties wechatProps, AdminRepository adminRepository, JwtUtil jwtUtil) {
        this.redis = redis;
        this.wechatAuthClient = wechatAuthClient;
        this.wechatProps = wechatProps;
        this.adminRepository = adminRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public QrcodeResponse createQrcode() {
        String state = IdUtil.fastSimpleUUID();
        writeSession(state, QrcodeCheckResponse.WAITING, null);
        return new QrcodeResponse(state, buildAuthorizeUrl(state), TTL_SECONDS);
    }

    @Override
    public QrcodeCheckResponse check(String state) {
        JSONObject session = readSession(state);
        if (session == null) {
            return QrcodeCheckResponse.of(QrcodeCheckResponse.EXPIRED);
        }
        String status = session.getStr("status");
        if (QrcodeCheckResponse.CONFIRMED.equals(status)) {
            LoginResponse login = JSONUtil.toBean(session.getJSONObject("login"), LoginResponse.class);
            redis.delete(SESSION_KEY + state);
            return QrcodeCheckResponse.confirmed(login);
        }
        return QrcodeCheckResponse.of(status);
    }

    @Override
    public void handleCallback(String state, String code) {
        if (!StringUtils.hasText(state) || readSession(state) == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "二维码已失效，请刷新重试");
        }
        String openid = wechatAuthClient.openCode2Openid(code);

        Admin admin = adminRepository.findByOpenid(openid);
        if (admin == null || !ValidFlag.ENABLED.is(admin.getIsValid())) {
            // openid 无匹配 / 未启用：后台无注册口，拒绝登录
            writeSession(state, QrcodeCheckResponse.REJECTED, null);
            return;
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.updateById(admin);

        String token = jwtUtil.issueAdminToken(admin.getId(), ValidFlag.ENABLED.is(admin.getIsSuper()));
        LoginResponse login = new LoginResponse(token, admin.getId(), admin.getNickname(), admin.getAvatarUrl());
        writeSession(state, QrcodeCheckResponse.CONFIRMED, login);
    }

    private String buildAuthorizeUrl(String state) {
        WechatProperties.Open open = wechatProps.getOpen();
        if (!StringUtils.hasText(open.getAppid())) {
            // 本地联调：直接给出回调地址，扫码端可手动带 code 触发
            return "/api/v1/admin/login/callback?state=" + state + "&code=mock_admin";
        }
        String redirect = urlEncode("https://域名/koala/api/v1/admin/login/callback");
        return "https://open.weixin.qq.com/connect/qrconnect"
                + "?appid=" + open.getAppid()
                + "&redirect_uri=" + redirect
                + "&response_type=code&scope=snsapi_login"
                + "&state=" + state + "#wechat_redirect";
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private void writeSession(String state, String status, LoginResponse login) {
        JSONObject json = new JSONObject();
        json.set("status", status);
        if (login != null) {
            json.set("login", login);
        }
        redis.opsForValue().set(SESSION_KEY + state, json.toString(), TTL_SECONDS, TimeUnit.SECONDS);
    }

    private JSONObject readSession(String state) {
        String raw = redis.opsForValue().get(SESSION_KEY + state);
        return StringUtils.hasText(raw) ? JSONUtil.parseObj(raw) : null;
    }
}
