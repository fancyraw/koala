package com.koala.enums;

/**
 * 售后类型。DB 存 int(code),实体字段仍为 Integer。
 * 待发货→仅退款;待收货→退货退款。
 */
public enum AfterSaleType {

    REFUND_ONLY(1, "仅退款"),
    RETURN_REFUND(2, "退货退款");

    private final int code;
    private final String label;

    AfterSaleType(int code, String label) {
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

    public static AfterSaleType of(Integer code) {
        if (code != null) {
            for (AfterSaleType t : values()) {
                if (t.code == code) {
                    return t;
                }
            }
        }
        return null;
    }
}
