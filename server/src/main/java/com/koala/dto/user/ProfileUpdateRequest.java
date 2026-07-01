package com.koala.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** C端更新资料(昵称/头像)。 */
@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称过长")
    private String nickname;

    @Size(max = 512, message = "头像URL过长")
    private String avatarUrl;
}
