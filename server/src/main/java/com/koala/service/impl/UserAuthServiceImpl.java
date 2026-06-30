package com.koala.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.common.auth.JwtUtil;
import com.koala.dto.auth.LoginResponse;
import com.koala.entity.User;
import com.koala.entity.UserAuth;
import com.koala.infra.wechat.WechatAuthClient;
import com.koala.mapper.UserAuthMapper;
import com.koala.mapper.UserMapper;
import com.koala.service.UserAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserAuthServiceImpl implements UserAuthService {

    private final WechatAuthClient wechatAuthClient;
    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final JwtUtil jwtUtil;

    public UserAuthServiceImpl(WechatAuthClient wechatAuthClient, UserMapper userMapper,
                               UserAuthMapper userAuthMapper, JwtUtil jwtUtil) {
        this.wechatAuthClient = wechatAuthClient;
        this.userMapper = userMapper;
        this.userAuthMapper = userAuthMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse loginByWechat(String code) {
        String openid = wechatAuthClient.mpCode2Openid(code);

        UserAuth auth = findAuth(openid);
        User user;
        if (auth == null) {
            user = register(openid);
        } else {
            user = userMapper.selectById(auth.getUserId());
        }

        String token = jwtUtil.issueUserToken(user.getId());
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl());
    }

    private UserAuth findAuth(String openid) {
        return userAuthMapper.selectOne(Wrappers.<UserAuth>lambdaQuery()
                .eq(UserAuth::getAuthType, UserAuth.TYPE_WECHAT_MP)
                .eq(UserAuth::getAuthId, openid));
    }

    private User register(String openid) {
        User user = new User();
        user.setNickname("微信用户");
        user.setAvatarUrl("");
        user.setIsValid(1);
        userMapper.insert(user);

        UserAuth auth = new UserAuth();
        auth.setUserId(user.getId());
        auth.setAuthType(UserAuth.TYPE_WECHAT_MP);
        auth.setAuthId(openid);
        try {
            userAuthMapper.insert(auth);
        } catch (DuplicateKeyException e) {
            // 并发首登：另一个事务已注册，回查既有记录
            UserAuth existing = findAuth(openid);
            return userMapper.selectById(existing.getUserId());
        }
        return user;
    }
}
