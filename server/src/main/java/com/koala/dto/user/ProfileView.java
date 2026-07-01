package com.koala.dto.user;

import com.koala.entity.User;
import lombok.Data;

/** C端个人资料。 */
@Data
public class ProfileView {

    private Long id;
    private String nickname;
    private String avatarUrl;

    public static ProfileView of(User user) {
        ProfileView v = new ProfileView();
        v.setId(user.getId());
        v.setNickname(user.getNickname());
        v.setAvatarUrl(user.getAvatarUrl());
        return v;
    }
}
