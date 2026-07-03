package com.koala.repository.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koala.entity.AfterSale;
import com.koala.enums.AfterSaleStatus;
import com.koala.mapper.AfterSaleMapper;
import com.koala.repository.AfterSaleRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public class AfterSaleRepositoryImpl implements AfterSaleRepository {

    private final AfterSaleMapper afterSaleMapper;

    public AfterSaleRepositoryImpl(AfterSaleMapper afterSaleMapper) {
        this.afterSaleMapper = afterSaleMapper;
    }

    @Override
    public void insert(AfterSale afterSale) {
        afterSaleMapper.insert(afterSale);
    }

    @Override
    public AfterSale findByNo(String afterSaleNo) {
        return afterSaleMapper.selectOne(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getAfterSaleNo, afterSaleNo));
    }

    @Override
    public long countActiveByOrderNo(String orderNo) {
        Long c = afterSaleMapper.selectCount(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getOrderNo, orderNo)
                .notIn(AfterSale::getStatus, AfterSaleStatus.REFUNDED.code(), AfterSaleStatus.REJECTED.code()));
        return c == null ? 0 : c;
    }

    @Override
    public long countByStatuses(Collection<Integer> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        Long c = afterSaleMapper.selectCount(Wrappers.<AfterSale>lambdaQuery()
                .in(AfterSale::getStatus, statuses));
        return c == null ? 0 : c;
    }

    @Override
    public IPage<AfterSale> pageByUser(Long userId, Integer status, long page, long size) {
        return afterSaleMapper.selectPage(new Page<>(page, size),
                Wrappers.<AfterSale>lambdaQuery()
                        .eq(AfterSale::getUserId, userId)
                        .eq(status != null, AfterSale::getStatus, status)
                        .orderByDesc(AfterSale::getId));
    }

    @Override
    public IPage<AfterSale> pageForAdmin(String keyword, Collection<Long> userIds, Integer status, long page, long size) {
        boolean hasKw = StrUtil.isNotBlank(keyword);
        return afterSaleMapper.selectPage(new Page<>(page, size),
                Wrappers.<AfterSale>lambdaQuery()
                        .eq(status != null, AfterSale::getStatus, status)
                        .and(hasKw, w -> {
                            w.like(AfterSale::getAfterSaleNo, keyword).or().like(AfterSale::getOrderNo, keyword);
                            if (userIds != null && !userIds.isEmpty()) {
                                w.or().in(AfterSale::getUserId, userIds);
                            }
                        })
                        .orderByDesc(AfterSale::getId));
    }

    @Override
    public int updateStatusCas(String afterSaleNo, int expectStatus, int newStatus, String auditRemark) {
        return afterSaleMapper.update(null, Wrappers.<AfterSale>lambdaUpdate()
                .set(AfterSale::getStatus, newStatus)
                .set(StrUtil.isNotBlank(auditRemark), AfterSale::getAuditRemark, auditRemark)
                .eq(AfterSale::getAfterSaleNo, afterSaleNo)
                .eq(AfterSale::getStatus, expectStatus));
    }

    @Override
    public int updateTracking(String afterSaleNo, String trackingNo, int newStatus, Collection<Integer> allowedStatuses) {
        return afterSaleMapper.update(null, Wrappers.<AfterSale>lambdaUpdate()
                .set(AfterSale::getReturnTrackingNo, trackingNo)
                .set(AfterSale::getStatus, newStatus)
                .eq(AfterSale::getAfterSaleNo, afterSaleNo)
                .in(AfterSale::getStatus, allowedStatuses));
    }
}
