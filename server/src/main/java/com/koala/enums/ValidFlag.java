package com.koala.enums;

/**
 * 通用启用/停用标志(is_valid)。DB 存 int(1启用/0停用),多张表复用。
 */
public enum ValidFlag {

    DISABLED(0),
    ENABLED(1);

    private final int code;

    ValidFlag(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean is(Integer flag) {
        return flag != null && flag == code;
    }

    /** 布尔转 code:true→1,false→0。 */
    public static int of(boolean enabled) {
        return enabled ? ENABLED.code : DISABLED.code;
    }

    public static boolean isEnabled(Integer flag) {
        return ENABLED.is(flag);
    }
}
