package com.koala.dto.admin;

import com.koala.entity.Admin;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台管理员列表项(含待审核)。 */
@Data
public class AdminView {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private boolean superAdmin;
    private Integer isValid;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static AdminView of(Admin admin) {
        AdminView v = new AdminView();
        v.setId(admin.getId());
        v.setNickname(admin.getNickname());
        v.setAvatarUrl(admin.getAvatarUrl());
        v.setSuperAdmin(admin.getIsSuper() != null && admin.getIsSuper() == 1);
        v.setIsValid(admin.getIsValid());
        v.setLastLoginAt(admin.getLastLoginAt());
        v.setCreatedAt(admin.getCreatedAt());
        return v;
    }
}
