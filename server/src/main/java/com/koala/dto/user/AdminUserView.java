package com.koala.dto.user;

import com.koala.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台用户列表项。 */
@Data
public class AdminUserView {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private Integer isValid;
    private LocalDateTime createdAt;

    public static AdminUserView of(User user) {
        AdminUserView v = new AdminUserView();
        v.setId(user.getId());
        v.setNickname(user.getNickname());
        v.setAvatarUrl(user.getAvatarUrl());
        v.setIsValid(user.getIsValid());
        v.setCreatedAt(user.getCreatedAt());
        return v;
    }
}
