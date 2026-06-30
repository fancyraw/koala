package com.koala.dto.auth;

import lombok.Data;

/**
 * 扫码轮询结果。status：waiting=待扫码 / scanned=已扫待确认 / confirmed=成功 /
 * expired=已过期 / rejected=未授权或账号未启用。
 */
@Data
public class QrcodeCheckResponse {

    private String status;
    private LoginResponse login;

    public static final String WAITING = "waiting";
    public static final String CONFIRMED = "confirmed";
    public static final String EXPIRED = "expired";
    public static final String REJECTED = "rejected";

    public static QrcodeCheckResponse of(String status) {
        QrcodeCheckResponse r = new QrcodeCheckResponse();
        r.setStatus(status);
        return r;
    }

    public static QrcodeCheckResponse confirmed(LoginResponse login) {
        QrcodeCheckResponse r = new QrcodeCheckResponse();
        r.setStatus(CONFIRMED);
        r.setLogin(login);
        return r;
    }
}
