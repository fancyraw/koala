package com.koala.repository;

import com.koala.entity.Admin;

import java.util.List;

public interface AdminRepository {

    Admin findById(Long id);

    /** 按微信 openid 查询,不存在返回 null。 */
    Admin findByOpenid(String openid);

    /** 指定 openid 是否已存在管理员。 */
    boolean existsByOpenid(String openid);

    /** 全部管理员,按 id 倒序。 */
    List<Admin> findAll();

    void insert(Admin admin);

    void updateById(Admin admin);
}
