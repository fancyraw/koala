package com.koala.service;

import com.koala.common.result.PageResult;
import com.koala.dto.aftersale.AdminAfterSaleView;
import com.koala.dto.aftersale.AfterSaleApplyRequest;
import com.koala.dto.aftersale.AfterSaleAuditRequest;
import com.koala.dto.aftersale.AfterSaleTrackingRequest;
import com.koala.dto.aftersale.AfterSaleView;

public interface AfterSaleService {

    /** 申请售后(6.5)：待发货→仅退款/待收货→退货退款；建单 + order→5售后中。返回售后单号。 */
    String apply(Long userId, AfterSaleApplyRequest req);

    /** 买家撤销申请(仅待审核可撤)：售后单→5关闭，order 5→原状态。 */
    void cancelByUser(Long userId, String afterSaleNo);

    /** 买家填写/修改寄回单号(退货退款,待寄回/已寄回阶段)：状态 1→2，单号以最新为准。 */
    void fillTracking(Long userId, AfterSaleTrackingRequest req);

    /** 我的售后列表(可按状态)。 */
    PageResult<AfterSaleView> myList(Long userId, Integer status, long page, long size);

    /** 售后详情(校验归属)。 */
    AfterSaleView detail(Long userId, String afterSaleNo);

    // ---- 后台 ----

    /** 后台售后列表(售后单号/订单号/买家昵称搜索 + 状态 + 分页)。 */
    PageResult<AdminAfterSaleView> adminList(String keyword, Integer status, long page, long size);

    /** 后台售后详情。 */
    AdminAfterSaleView adminDetail(String afterSaleNo);

    /** 审核(仅待审核)：拒绝→5关闭+order回原状态；同意→仅退款直接退款(→4)，退货退款→1待寄回。 */
    void audit(AfterSaleAuditRequest req);

    /** 确认收货并退款(退货退款,买家已寄回2→3→退款4)。 */
    void confirmReceive(String afterSaleNo);
}
