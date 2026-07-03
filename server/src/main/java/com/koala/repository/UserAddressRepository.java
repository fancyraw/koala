package com.koala.repository;

import com.koala.entity.UserAddress;

import java.util.List;

public interface UserAddressRepository {

    UserAddress findById(Long id);

    /** 用户地址,默认地址优先、id 倒序。 */
    List<UserAddress> findByUser(Long userId);

    long countByUser(Long userId);

    void insert(UserAddress address);

    void updateById(UserAddress address);

    void deleteById(Long id);

    /** 清除用户当前默认地址标记(is_default 1→0)。 */
    void clearDefault(Long userId);
}
