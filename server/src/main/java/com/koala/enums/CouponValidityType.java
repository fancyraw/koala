package com.koala.enums;

/**
 * 券有效期类型。DB 存 int(code)。固定区间需起止时间;领取后N天需有效天数。
 */
public enum CouponValidityType {

    FIXED_RANGE(1, "固定区间"),
    DAYS_AFTER_GRANT(2, "领取后N天");

    private final int code;
    private final String label;

    CouponValidityType(int code, String label) {
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

    public static CouponValidityType of(Integer code) {
        if (code != null) {
            for (CouponValidityType t : values()) {
                if (t.code == code) {
                    return t;
                }
            }
        }
        return null;
    }
}
