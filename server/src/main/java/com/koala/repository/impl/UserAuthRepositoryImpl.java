package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.UserAuth;
import com.koala.mapper.UserAuthMapper;
import com.koala.repository.UserAuthRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserAuthRepositoryImpl implements UserAuthRepository {

    private final UserAuthMapper userAuthMapper;

    public UserAuthRepositoryImpl(UserAuthMapper userAuthMapper) {
        this.userAuthMapper = userAuthMapper;
    }

    @Override
    public UserAuth findByTypeAndAuthId(String authType, String authId) {
        return userAuthMapper.selectOne(Wrappers.<UserAuth>lambdaQuery()
                .eq(UserAuth::getAuthType, authType)
                .eq(UserAuth::getAuthId, authId));
    }

    @Override
    public void insert(UserAuth auth) {
        userAuthMapper.insert(auth);
    }
}
