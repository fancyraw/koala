package com.koala.enums;

/**
 * 用户券状态。DB 存 int(code)。锁定态(3)对用户隐藏(下单占用中)。
 */
public enum UserCouponStatus {

    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期"),
    LOCKED(3, "锁定");

    private final int code;
    private final String label;

    UserCouponStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean is(Integer status) {
        return status != null && status == code;
    }

    public static UserCouponStatus of(Integer code) {
        if (code != null) {
            for (UserCouponStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
        }
        return null;
    }
}
