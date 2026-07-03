package com.koala.repository;

import com.koala.entity.UserAuth;

public interface UserAuthRepository {

    /** 按认证类型 + 认证标识查询,不存在返回 null。 */
    UserAuth findByTypeAndAuthId(String authType, String authId);

    void insert(UserAuth auth);
}
