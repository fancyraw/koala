package com.koala.enums;

/**
 * 订单状态。DB 存 int(code),实体字段仍为 Integer;业务层用本枚举替代硬编码字面量。
 */
public enum OrderStatus {

    WAIT_PAY(0, "待付款"),
    WAIT_SHIP(1, "待发货"),
    WAIT_RECEIVE(2, "待收货"),
    COMPLETED(3, "已完成"),
    CANCELED(4, "已取消"),
    AFTER_SALE(5, "售后中"),
    REFUNDED(6, "已退款");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
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

    public static OrderStatus of(Integer code) {
        if (code != null) {
            for (OrderStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
        }
        return null;
    }
}
