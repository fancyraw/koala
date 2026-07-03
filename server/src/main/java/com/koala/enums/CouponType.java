package com.koala.enums;

/**
 * 券类型(业务仅两类)。DB 存 int(code)。
 */
public enum CouponType {

    FULL_REDUCE(1, "满减券"),
    NO_THRESHOLD(2, "无门槛券");

    private final int code;
    private final String label;

    CouponType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean is(Integer type) {
        return type != null && type == code;
    }

    public static CouponType of(Integer code) {
        if (code != null) {
            for (CouponType t : values()) {
                if (t.code == code) {
                    return t;
                }
            }
        }
        return null;
    }
}
