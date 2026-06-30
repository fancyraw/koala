package com.koala.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserLoginRequest {

    @NotBlank(message = "code 不能为空")
    private String code;
}
