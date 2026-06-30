package com.koala.common.auth;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;

/**
 * 请求级登录主体上下文，拦截器写入、Controller/Service 读取，请求结束清理。
 */
public final class AuthContext {

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(Principal principal) {
        HOLDER.set(principal);
    }

    public static Principal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 取当前登录主体，未登录抛 UNAUTHORIZED。 */
    public static Principal require() {
        Principal p = HOLDER.get();
        if (p == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return p;
    }

    /** 取当前 C 端用户ID，非 C 端登录抛异常。 */
    public static Long requireUserId() {
        Principal p = require();
        if (!p.isUser()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return p.getId();
    }

    /** 取当前后台管理员ID，非后台登录抛异常。 */
    public static Long requireAdminId() {
        Principal p = require();
        if (!p.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return p.getId();
    }
}
