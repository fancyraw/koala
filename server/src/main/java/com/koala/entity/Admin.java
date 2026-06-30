package com.koala.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.koala.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin")
public class Admin extends BaseEntity {

    private Long id;
    private String wxOpenid;
    private String nickname;
    private String avatarUrl;
    private Integer isSuper;
    private Integer isValid;
    private LocalDateTime lastLoginAt;
}
