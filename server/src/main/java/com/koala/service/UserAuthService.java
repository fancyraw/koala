package com.koala.service;

import com.koala.common.auth.JwtUtil;
import com.koala.dto.auth.LoginResponse;
import com.koala.entity.User;
import com.koala.entity.UserAuth;
import com.koala.enums.ValidFlag;
import com.koala.infra.wechat.WechatAuthClient;
import com.koala.repository.UserAuthRepository;
import com.koala.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserAuthService {

    private final WechatAuthClient wechatAuthClient;
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final JwtUtil jwtUtil;

    public UserAuthService(WechatAuthClient wechatAuthClient, UserRepository userRepository,
                               UserAuthRepository userAuthRepository, JwtUtil jwtUtil) {
        this.wechatAuthClient = wechatAuthClient;
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse loginByWechat(String code) {
        String openid = wechatAuthClient.mpCode2Openid(code);

        UserAuth auth = findAuth(openid);
        User user;
        if (auth == null) {
            user = register(openid);
        } else {
            user = userRepository.findById(auth.getUserId());
        }

        String token = jwtUtil.issueUserToken(user.getId());
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl());
    }

    private UserAuth findAuth(String openid) {
        return userAuthRepository.findByTypeAndAuthId(UserAuth.TYPE_WECHAT_MP, openid);
    }

    private User register(String openid) {
        User user = new User();
        user.setNickname("微信用户");
        user.setAvatarUrl("");
        user.setIsValid(ValidFlag.ENABLED.code());
        userRepository.insert(user);

        UserAuth auth = new UserAuth();
        auth.setUserId(user.getId());
        auth.setAuthType(UserAuth.TYPE_WECHAT_MP);
        auth.setAuthId(openid);
        try {
            userAuthRepository.insert(auth);
        } catch (DuplicateKeyException e) {
            // 并发首登：另一个事务已注册，回查既有记录
            UserAuth existing = findAuth(openid);
            return userRepository.findById(existing.getUserId());
        }
        return user;
    }
}
