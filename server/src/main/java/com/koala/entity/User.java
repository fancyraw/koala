package com.koala.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.koala.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private Integer isValid;
}
