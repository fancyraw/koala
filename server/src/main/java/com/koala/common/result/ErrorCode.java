package com.koala.common.result;

/**
 * 错误码分段：0 成功 / 1xxx 鉴权 / 2xxx 参数 / 3xxx 业务 / 5xxx 系统。
 */
public enum ErrorCode {

    SUCCESS(0, "ok"),

    // 1xxx 鉴权
    UNAUTHORIZED(1001, "未登录或登录已失效"),
    TOKEN_INVALID(1002, "登录凭证无效"),
    TOKEN_EXPIRED(1003, "登录已过期"),
    FORBIDDEN(1004, "无权访问"),
    ACCOUNT_DISABLED(1005, "账号已被禁用"),

    // 2xxx 参数
    PARAM_ERROR(2001, "参数错误"),
    PARAM_MISSING(2002, "缺少必要参数"),

    // 3xxx 业务
    BIZ_ERROR(3000, "业务处理失败"),
    DATA_NOT_FOUND(3001, "数据不存在"),
    DUPLICATE_SUBMIT(3002, "请勿重复提交"),
    STOCK_NOT_ENOUGH(3003, "库存不足"),
    COUPON_UNAVAILABLE(3004, "优惠券不可用"),
    COUPON_SOLD_OUT(3005, "优惠券已发完"),
    ORDER_STATUS_ERROR(3006, "订单状态不允许此操作"),
    PURCHASE_LIMIT(3007, "超过单次限购数量"),
    AFTER_SALE_STATUS_ERROR(3008, "售后单状态不允许此操作"),
    PAY_FAILED(3009, "支付失败"),
    REFUND_FAILED(3010, "退款失败"),
    ADDRESS_INVALID(3011, "收货地址不合法"),

    // 5xxx 系统
    SYSTEM_ERROR(5000, "系统繁忙，请稍后再试"),
    MAINTENANCE(5001, "系统维护中，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
