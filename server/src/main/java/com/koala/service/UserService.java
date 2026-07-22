package com.koala.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.common.result.PageResult;
import com.koala.dto.user.AdminUserDetailView;
import com.koala.dto.user.AdminUserView;
import com.koala.dto.user.ProfileUpdateRequest;
import com.koala.dto.user.ProfileView;
import com.koala.entity.Order;
import com.koala.entity.User;
import com.koala.enums.ValidFlag;
import com.koala.repository.OrderRepository;
import com.koala.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UserService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public ProfileView profile(Long userId) {
        return ProfileView.of(requireUser(userId));
    }

    public ProfileView updateProfile(Long userId, ProfileUpdateRequest req) {
        requireUser(userId);
        User patch = new User();
        patch.setId(userId);
        patch.setNickname(req.getNickname());
        patch.setAvatarUrl(req.getAvatarUrl() != null ? req.getAvatarUrl() : "");
        userRepository.updateById(patch);
        return ProfileView.of(requireUser(userId));
    }

    public PageResult<AdminUserView> listForAdmin(String keyword, Integer status, long page, long size) {
        IPage<User> p = userRepository.pageForAdmin(keyword, status, page, size);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        List<AdminUserView> list = p.getRecords().stream()
                .map(AdminUserView::of).collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public AdminUserDetailView detailForAdmin(Long id) {
        AdminUserDetailView v = AdminUserDetailView.of(requireUser(id));
        List<Order> paidOrders = orderRepository.findPaidByUser(id);
        v.setPaidOrderCount(paidOrders.size());
        v.setTotalPaidAmount(paidOrders.stream()
                .map(Order::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return v;
    }

    public void setStatus(Long id, boolean valid) {
        requireUser(id);
        User patch = new User();
        patch.setId(id);
        patch.setIsValid(ValidFlag.of(valid));
        userRepository.updateById(patch);
    }

    private User requireUser(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return user;
    }
}
