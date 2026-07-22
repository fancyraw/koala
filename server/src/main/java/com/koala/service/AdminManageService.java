package com.koala.service;

import cn.hutool.core.util.IdUtil;
import com.koala.common.constant.RedisKeys;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.config.WechatProperties;
import com.koala.dto.admin.AcceptInviteRequest;
import com.koala.dto.admin.AdminView;
import com.koala.dto.admin.InviteResponse;
import com.koala.entity.Admin;
import com.koala.enums.ValidFlag;
import com.koala.infra.wechat.WechatAuthClient;
import com.koala.repository.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 管理员管理(6.10)：一次性邀请 token 存 Redis(TTL 30min),扫码入库待审核,超管启用。 */
@Slf4j
@Service
public class AdminManageService {

    private static final long TTL_SECONDS = 30 * 60;

    private final StringRedisTemplate redis;
    private final WechatProperties wechatProps;
    private final WechatAuthClient wechatAuthClient;
    private final AdminRepository adminRepository;

    public AdminManageService(StringRedisTemplate redis, WechatProperties wechatProps,
                                  WechatAuthClient wechatAuthClient, AdminRepository adminRepository) {
        this.redis = redis;
        this.wechatProps = wechatProps;
        this.wechatAuthClient = wechatAuthClient;
        this.adminRepository = adminRepository;
    }

    public InviteResponse invite() {
        String token = IdUtil.fastSimpleUUID();
        redis.opsForValue().set(RedisKeys.ADMIN_INVITE_TOKEN + token, "1", TTL_SECONDS, TimeUnit.SECONDS);
        return new InviteResponse(token, buildInviteUrl(token), TTL_SECONDS);
    }

    public void accept(AcceptInviteRequest req) {
        // 一次性消费 token：delete 返回 true 表示存在且本次抢到，转发重放只有一次成功
        Boolean consumed = redis.delete(RedisKeys.ADMIN_INVITE_TOKEN + req.getInviteToken());
        if (!Boolean.TRUE.equals(consumed)) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "邀请链接已失效或已被使用");
        }
        String openid = wechatAuthClient.openCode2Openid(req.getCode());
        if (adminRepository.existsByOpenid(openid)) {
            throw new BizException(ErrorCode.DUPLICATE_SUBMIT, "该微信已是管理员");
        }
        Admin admin = new Admin();
        admin.setWxOpenid(openid);
        admin.setNickname("待审核管理员");
        admin.setAvatarUrl("");
        admin.setIsSuper(ValidFlag.DISABLED.code());
        admin.setIsValid(ValidFlag.DISABLED.code());
        adminRepository.insert(admin);
    }

    public List<AdminView> list() {
        return adminRepository.findAll()
                .stream().map(AdminView::of).collect(Collectors.toList());
    }

    public void setStatus(Long id, boolean valid, Long operatorId) {
        if (id.equals(operatorId)) {
            throw new BizException(ErrorCode.BIZ_ERROR, "不能修改自身状态");
        }
        Admin admin = adminRepository.findById(id);
        if (admin == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (ValidFlag.ENABLED.is(admin.getIsSuper())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "不能禁用/修改超级管理员");
        }
        Admin patch = new Admin();
        patch.setId(id);
        patch.setIsValid(ValidFlag.of(valid));
        adminRepository.updateById(patch);
    }

    private String buildInviteUrl(String token) {
        WechatProperties.Open open = wechatProps.getOpen();
        if (!StringUtils.hasText(open.getAppid())) {
            // 本地联调：直接给回调地址，扫码端手动带 code 触发 accept
            return "/api/v1/admin/admins/accept?inviteToken=" + token + "&code=mock_admin_new";
        }
        String redirect = urlEncode("https://域名/koala/admin/invite/callback?inviteToken=" + token);
        return "https://open.weixin.qq.com/connect/qrconnect"
                + "?appid=" + open.getAppid()
                + "&redirect_uri=" + redirect
                + "&response_type=code&scope=snsapi_login"
                + "&state=" + token + "#wechat_redirect";
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
