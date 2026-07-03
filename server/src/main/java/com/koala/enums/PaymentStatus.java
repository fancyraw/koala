package com.koala.enums;

/**
 * 支付流水状态。DB 存 int(code)。
 */
public enum PaymentStatus {

    PENDING(0, "待支付"),
    SUCCESS(1, "支付成功"),
    FAILED(2, "支付失败"),
    REFUNDED(3, "已退款");

    private final int code;
    private final String label;

    PaymentStatus(int code, String label) {
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

    public static PaymentStatus of(Integer code) {
        if (code != null) {
            for (PaymentStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
        }
        return null;
    }
}
