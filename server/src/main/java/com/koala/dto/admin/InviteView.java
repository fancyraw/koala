package com.koala.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 超管生成的一次性邀请二维码/链接。 */
@Data
@AllArgsConstructor
public class InviteView {

    /** 一次性邀请 token(校验用)。 */
    private String inviteToken;
    /** 邀请二维码内容(新人扫码授权 URL,内含 token)。 */
    private String qrcodeUrl;
    /** 有效秒数。 */
    private long expireSeconds;
}
