package com.koala.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long id;
    private String nickname;
    private String avatarUrl;
}
