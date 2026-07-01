package com.koala.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.common.result.PageResult;
import com.koala.dto.user.AdminUserDetailView;
import com.koala.dto.user.AdminUserView;
import com.koala.dto.user.ProfileUpdateRequest;
import com.koala.dto.user.ProfileView;
import com.koala.entity.Order;
import com.koala.entity.User;
import com.koala.mapper.OrderMapper;
import com.koala.mapper.UserMapper;
import com.koala.service.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;

    public UserServiceImpl(UserMapper userMapper, OrderMapper orderMapper) {
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    public ProfileView profile(Long userId) {
        return ProfileView.of(requireUser(userId));
    }

    @Override
    public ProfileView updateProfile(Long userId, ProfileUpdateRequest req) {
        requireUser(userId);
        User patch = new User();
        patch.setId(userId);
        patch.setNickname(req.getNickname());
        patch.setAvatarUrl(req.getAvatarUrl() != null ? req.getAvatarUrl() : "");
        userMapper.updateById(patch);
        return ProfileView.of(requireUser(userId));
    }

    @Override
    public PageResult<AdminUserView> listForAdmin(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<User> q = Wrappers.<User>lambdaQuery()
                .like(StrUtil.isNotBlank(keyword), User::getNickname, keyword)
                .eq(status != null, User::getIsValid, status)
                .orderByDesc(User::getId);
        IPage<User> p = userMapper.selectPage(new Page<>(page, size), q);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        List<AdminUserView> list = p.getRecords().stream()
                .map(AdminUserView::of).collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public AdminUserDetailView detailForAdmin(Long id) {
        AdminUserDetailView v = AdminUserDetailView.of(requireUser(id));
        List<Order> paidOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, id)
                .isNotNull(Order::getPaidAt));
        v.setPaidOrderCount(paidOrders.size());
        v.setTotalPaidAmount(paidOrders.stream()
                .map(Order::getPayAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return v;
    }

    @Override
    public void setStatus(Long id, boolean valid) {
        requireUser(id);
        User patch = new User();
        patch.setId(id);
        patch.setIsValid(valid ? 1 : 0);
        userMapper.updateById(patch);
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return user;
    }
}
