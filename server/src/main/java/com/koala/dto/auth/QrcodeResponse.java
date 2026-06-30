package com.koala.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QrcodeResponse {

    /** 轮询用的登录会话标识 */
    private String state;
    /** 二维码内容（微信开放平台授权 URL，前端渲染成二维码供扫描） */
    private String qrcodeUrl;
    /** 会话有效秒数 */
    private long expireSeconds;
}
