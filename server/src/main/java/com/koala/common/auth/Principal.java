package com.koala.common.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前登录主体，由 JWT 解析得到，存入 {@link AuthContext}。
 * C 端只用 id；后台用 id + isSuper。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Principal implements Serializable {

    /** 主体类型：user(C端) / admin(后台) */
    private String type;
    /** 主体ID：C端为 userId，后台为 adminId */
    private Long id;
    /** 后台是否超管，C 端恒 false */
    private boolean superAdmin;

    public static final String TYPE_USER = "user";
    public static final String TYPE_ADMIN = "admin";

    public boolean isUser() {
        return TYPE_USER.equals(type);
    }

    public boolean isAdmin() {
        return TYPE_ADMIN.equals(type);
    }
}
