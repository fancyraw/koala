package com.koala.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 新人扫码入库：一次性 token + 微信授权 code。 */
@Data
public class AcceptInviteRequest {

    @NotBlank(message = "邀请token不能为空")
    private String inviteToken;

    @NotBlank(message = "code不能为空")
    private String code;
}
