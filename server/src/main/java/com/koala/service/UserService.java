package com.koala.service;

import com.koala.common.result.PageResult;
import com.koala.dto.user.AdminUserDetailView;
import com.koala.dto.user.AdminUserView;
import com.koala.dto.user.ProfileUpdateRequest;
import com.koala.dto.user.ProfileView;

public interface UserService {

    /** C端：查询个人资料。 */
    ProfileView profile(Long userId);

    /** C端：更新昵称/头像，返回最新资料。 */
    ProfileView updateProfile(Long userId, ProfileUpdateRequest req);

    /** 后台：用户列表(昵称模糊/状态过滤，分页)。 */
    PageResult<AdminUserView> listForAdmin(String keyword, Integer status, long page, long size);

    /** 后台：用户详情(含下单统计)。 */
    AdminUserDetailView detailForAdmin(Long id);

    /** 后台：禁用/启用用户(禁用后请求侧即时失效)。 */
    void setStatus(Long id, boolean valid);
}
