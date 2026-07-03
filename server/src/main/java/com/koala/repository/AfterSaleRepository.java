package com.koala.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koala.entity.AfterSale;

import java.util.Collection;

/**
 * 售后单数据访问(见"抽 repository 层")。封装 MyBatis-Plus Wrappers,
 * service 层不再直接依赖 Mapper,只面向领域方法。
 */
public interface AfterSaleRepository {

    void insert(AfterSale afterSale);

    /** 按售后单号查询,不存在返回 null。 */
    AfterSale findByNo(String afterSaleNo);

    /** 同订单进行中的售后单数量(排除终态:已退款/已拒绝)。 */
    long countActiveByOrderNo(String orderNo);

    /** 处于指定状态集合的售后单数量(看板待处理统计)。 */
    long countByStatuses(Collection<Integer> statuses);

    IPage<AfterSale> pageByUser(Long userId, Integer status, long page, long size);

    /**
     * 后台分页:关键字命中售后单号/订单号,或命中传入的用户 id 集合。
     * @param userIds 昵称匹配得到的用户 id(空集表示不按用户过滤)
     */
    IPage<AfterSale> pageForAdmin(String keyword, Collection<Long> userIds, Integer status, long page, long size);

    /** CAS 更新状态:仅当 expectStatus 命中时置为 newStatus,并可选写审核备注。返回受影响行数。 */
    int updateStatusCas(String afterSaleNo, int expectStatus, int newStatus, String auditRemark);

    /** 填写寄回单号并置为"买家已寄回":状态须为待寄回/已寄回。返回受影响行数。 */
    int updateTracking(String afterSaleNo, String trackingNo, int newStatus, Collection<Integer> allowedStatuses);
}
