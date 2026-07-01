package com.koala.service;

import com.koala.common.result.PageResult;
import com.koala.dto.order.AdminOrderView;
import com.koala.dto.order.OrderPreviewRequest;
import com.koala.dto.order.OrderPreviewView;
import com.koala.dto.order.OrderRefundRequest;
import com.koala.dto.order.OrderShipRequest;
import com.koala.dto.order.OrderSubmitRequest;
import com.koala.dto.order.OrderSubmitView;
import com.koala.dto.order.OrderView;
import com.koala.infra.pay.PrepayResult;

import javax.servlet.http.HttpServletRequest;

public interface OrderService {

    /** 算价预览(6.2)：与提交共用算价，含凑单提示。 */
    OrderPreviewView preview(Long userId, OrderPreviewRequest req);

    /** 提交订单(6.3)：幂等 + 扣库存 + 锁券 + 建单，返回订单号与实付。 */
    OrderSubmitView submit(Long userId, OrderSubmitRequest req);

    /** 取支付参数(调渠道 prepay)。 */
    PrepayResult pay(Long userId, String orderNo);

    /** 支付回调(6.4)：验签 → 幂等 → 释放/核销。返回是否处理成功。 */
    boolean payNotify(HttpServletRequest request);

    /** 我的订单列表(可按状态)。 */
    PageResult<OrderView> myOrders(Long userId, Integer status, long page, long size);

    /** 订单详情(校验归属)。 */
    OrderView detail(Long userId, String orderNo);

    /** 取消订单(6.4 释放资产)：仅待付款可取消。 */
    void cancel(Long userId, String orderNo);

    /** 确认收货(幂等)：待收货 → 已完成。 */
    void confirm(Long userId, String orderNo);

    /** C端删除订单(软删)：仅已完成/已取消/已退款可删。 */
    void deleteByUser(Long userId, String orderNo);

    // ---- 后台 ----

    /** 后台订单列表(订单号/买家昵称/收货人搜索 + 状态 + 分页)。 */
    PageResult<AdminOrderView> adminList(String keyword, Integer status, long page, long size);

    /** 后台订单详情。 */
    AdminOrderView adminDetail(String orderNo);

    /** 发货：待发货 → 待收货。 */
    void ship(OrderShipRequest req);

    /** 已完成订单手动发起退款(6.5)：5售后中 → 6已退款。 */
    void adminRefund(OrderRefundRequest req);

    /** 售后退款(6.5)：订单须为 5售后中 → 调渠道退款 → 券原路退回 → 5→6 → 发事件。供售后模块复用。 */
    void refundForAfterSale(String orderNo, String reason);
}
