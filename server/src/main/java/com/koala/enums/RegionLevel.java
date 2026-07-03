package com.koala.enums;

/**
 * 行政区划层级。DB 存 int(code):省/市/区县。
 */
public enum RegionLevel {

    PROVINCE(1),
    CITY(2),
    DISTRICT(3);

    private final int code;

    RegionLevel(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean is(Integer level) {
        return level != null && level == code;
    }
}
