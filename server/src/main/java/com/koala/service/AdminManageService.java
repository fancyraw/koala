package com.koala.service;

import com.koala.dto.admin.AcceptInviteRequest;
import com.koala.dto.admin.AdminView;
import com.koala.dto.admin.InviteResponse;

import java.util.List;

/** 管理员管理(仅超管)：邀请/入库/列表/启用禁用。见 6.10。 */
public interface AdminManageService {

    /** 超管生成一次性邀请二维码/链接(token 存 Redis,TTL 30min)。 */
    InviteResponse invite();

    /** 新人扫码入库(无需登录)：校验 token + openid 未注册 → 建号 is_valid=0 待审核。 */
    void accept(AcceptInviteRequest req);

    /** 管理员列表(含待审核)。 */
    List<AdminView> list();

    /** 超管启用/禁用管理员(禁用后其请求即时失效;不可禁用自己/其他超管)。 */
    void setStatus(Long id, boolean valid, Long operatorId);
}
