package com.koala.repository.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koala.entity.User;
import com.koala.mapper.UserMapper;
import com.koala.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<Long> findIdsByNicknameLike(String keyword) {
        return userMapper.selectList(Wrappers.<User>lambdaQuery()
                        .like(User::getNickname, keyword).select(User::getId))
                .stream().map(User::getId).collect(Collectors.toList());
    }

    @Override
    public List<User> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(ids);
    }

    @Override
    public IPage<User> pageForAdmin(String keyword, Integer isValid, long page, long size) {
        return userMapper.selectPage(new Page<>(page, size),
                Wrappers.<User>lambdaQuery()
                        .like(StrUtil.isNotBlank(keyword), User::getNickname, keyword)
                        .eq(isValid != null, User::getIsValid, isValid)
                        .orderByDesc(User::getId));
    }

    @Override
    public long countNewUsers(LocalDateTime start, LocalDateTime end) {
        Long c = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreatedAt, start)
                .lt(User::getCreatedAt, end));
        return c == null ? 0 : c;
    }

    @Override
    public void insert(User user) {
        userMapper.insert(user);
    }

    @Override
    public void updateById(User user) {
        userMapper.updateById(user);
    }
}
