package com.koala.infra.wechat;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.config.WechatProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信凭证换取 openid。C 端走小程序 jscode2session；后台走开放平台 oauth2。
 * 未配置 appid 且开启 mock 时，直接用 code 当 openid，便于本地联调。
 */
@Slf4j
@Component
public class WechatAuthClient {

    private static final String MP_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String OPEN_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";

    private final WechatProperties props;

    public WechatAuthClient(WechatProperties props) {
        this.props = props;
    }

    /** C 端小程序 code → openid。 */
    public String mpCode2Openid(String code) {
        WechatProperties.Mp mp = props.getMp();
        if (!StringUtils.hasText(mp.getAppid())) {
            return mockOpenid(code, "mp");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("appid", mp.getAppid());
        params.put("secret", mp.getSecret());
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");
        JSONObject json = request(MP_URL, params);
        String openid = json.getStr("openid");
        if (!StringUtils.hasText(openid)) {
            log.warn("微信 jscode2session 无 openid: {}", json);
            throw new BizException(ErrorCode.TOKEN_INVALID, "微信登录失败");
        }
        return openid;
    }

    /** 后台开放平台扫码 code → openid。 */
    public String openCode2Openid(String code) {
        WechatProperties.Open open = props.getOpen();
        if (!StringUtils.hasText(open.getAppid())) {
            return mockOpenid(code, "open");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("appid", open.getAppid());
        params.put("secret", open.getSecret());
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        JSONObject json = request(OPEN_TOKEN_URL, params);
        String openid = json.getStr("openid");
        if (!StringUtils.hasText(openid)) {
            log.warn("微信开放平台换 openid 失败: {}", json);
            throw new BizException(ErrorCode.TOKEN_INVALID, "微信扫码登录失败");
        }
        return openid;
    }

    private JSONObject request(String url, Map<String, Object> params) {
        try {
            String body = HttpUtil.get(url, params, 5000);
            JSONObject json = JSONUtil.parseObj(body);
            Integer errcode = json.getInt("errcode");
            if (errcode != null && errcode != 0) {
                log.warn("微信接口返回错误 errcode={}, errmsg={}", errcode, json.getStr("errmsg"));
                throw new BizException(ErrorCode.TOKEN_INVALID, "微信登录失败");
            }
            return json;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信接口异常", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信服务不可用");
        }
    }

    private String mockOpenid(String code, String scene) {
        if (!props.isMockWhenUnconfigured()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信" + scene + "未配置");
        }
        if (!StringUtils.hasText(code)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "code 不能为空");
        }
        log.warn("微信{}未配置，使用 mock openid（仅限本地联调）", scene);
        return "mock_" + scene + "_" + code;
    }
}
