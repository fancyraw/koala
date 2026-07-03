package com.koala.enums;

/**
 * 售后单状态。DB 存 int(code),实体字段仍为 Integer;业务层用本枚举替代硬编码字面量。
 */
public enum AfterSaleStatus {

    PENDING_AUDIT(0, "待审核"),
    APPROVED_WAIT_RETURN(1, "通过待寄回"),
    BUYER_RETURNED(2, "买家已寄回"),
    MERCHANT_RECEIVED(3, "商家已收货"),
    REFUNDED(4, "已退款"),
    REJECTED(5, "已拒绝");

    private final int code;
    private final String label;

    AfterSaleStatus(int code, String label) {
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

    public static AfterSaleStatus of(Integer code) {
        if (code != null) {
            for (AfterSaleStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
        }
        return null;
    }
}
