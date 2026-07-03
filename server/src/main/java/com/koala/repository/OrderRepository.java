package com.koala.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koala.entity.Order;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrderRepository {

    /** 按订单号查询,不存在返回 null。 */
    Order findByNo(String orderNo);

    void insert(Order order);

    /** C 端分页:未删除,可选状态,按 id 倒序。 */
    IPage<Order> pageByUser(Long userId, Integer status, long page, long size);

    /** 后台分页:可选状态 + 关键字(订单号/收货人/命中的 userId 集合),按 id 倒序。 */
    IPage<Order> pageForAdmin(Integer status, String keyword, Collection<Long> userIds, long page, long size);

    /** 待付款(0)且已超时的订单。 */
    List<Order> findTimedOutUnpaid(LocalDateTime now);

    /** 待收货(2)且发货时间早于 deadline 的订单。 */
    List<Order> findAutoConfirmDue(LocalDateTime deadline);

    /** 指定用户全部已支付订单(paid_at 非空)。 */
    List<Order> findPaidByUser(Long userId);

    /** paid_at >= since 的全部已支付订单(看板窗口聚合)。 */
    List<Order> findPaidSince(LocalDateTime since);

    /** 指定状态的订单数量。 */
    long countByStatus(int status);

    /** CAS 更新订单状态:仅当 expectStatus 命中时置为 newStatus。返回受影响行数。 */
    int updateStatusCas(String orderNo, int expectStatus, int newStatus);

    /** 支付成功:待付款(0)→待发货(1) 并写 paidAt。返回受影响行数。 */
    int markPaidCas(String orderNo, LocalDateTime paidAt);

    /** 确认收货:待收货(2)→已完成(3) 并写 completedAt。返回受影响行数。 */
    int markCompletedCas(String orderNo, LocalDateTime completedAt);

    /** 发货:待发货(1)→待收货(2) 并写物流与发货时间。返回受影响行数。 */
    int markShippedCas(String orderNo, String logisticsCompany, String logisticsNo, LocalDateTime shippedAt);

    /** 释放资产收尾:待付款(0)→targetStatus 并写 canceledAt。返回受影响行数。 */
    int cancelFromUnpaidCas(String orderNo, int targetStatus, LocalDateTime canceledAt);

    /** 用户软删除:标记 user_deleted=1。 */
    void markUserDeleted(String orderNo);
}
