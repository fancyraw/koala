package com.koala.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koala.entity.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface UserRepository {

    User findById(Long id);

    /** 昵称模糊匹配,返回用户 id 列表。 */
    List<Long> findIdsByNicknameLike(String keyword);

    List<User> findByIds(Collection<Long> ids);

    /** 后台分页:可选昵称关键字 + 有效状态,按 id 倒序。 */
    IPage<User> pageForAdmin(String keyword, Integer isValid, long page, long size);

    /** 指定时间区间 [start, end) 内新注册用户数。 */
    long countNewUsers(LocalDateTime start, LocalDateTime end);

    void insert(User user);

    void updateById(User user);
}
