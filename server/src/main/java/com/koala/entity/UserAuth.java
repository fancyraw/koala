package com.koala.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_auth")
public class UserAuth implements Serializable {

    public static final String TYPE_WECHAT_MP = "wechat_mp";

    private Long id;
    private Long userId;
    private String authType;
    private String authId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
