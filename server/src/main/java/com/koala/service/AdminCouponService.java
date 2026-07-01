package com.koala.service;

import com.koala.common.result.PageResult;
import com.koala.dto.coupon.AdminCouponView;
import com.koala.dto.coupon.CouponSaveRequest;
import com.koala.dto.coupon.GrantDetailView;

import java.util.List;

public interface AdminCouponService {

    /** 券模板列表：state 可空(全部)或按派生态过滤；分页。 */
    PageResult<AdminCouponView> list(String state, long page, long size);

    /** 券详情。 */
    AdminCouponView detail(Long id);

    /** 新增/编辑券模板，返回券模板id。 */
    Long save(CouponSaveRequest req, Long adminId);

    /** 停发：置 is_valid=0，终态不可逆。 */
    void stop(Long id);

    /** 删除：仅无下发记录(issued_count=0)可删。 */
    void delete(Long id);

    /** 券发放明细（某券模板下发的所有用户券）。 */
    List<GrantDetailView> grants(Long couponId);
}
