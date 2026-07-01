package com.koala.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.common.result.PageResult;
import com.koala.dto.aftersale.AdminAfterSaleView;
import com.koala.dto.aftersale.AfterSaleApplyRequest;
import com.koala.dto.aftersale.AfterSaleAuditRequest;
import com.koala.dto.aftersale.AfterSaleTrackingRequest;
import com.koala.dto.aftersale.AfterSaleView;
import com.koala.entity.AfterSale;
import com.koala.entity.Order;
import com.koala.entity.User;
import com.koala.mapper.AfterSaleMapper;
import com.koala.mapper.OrderMapper;
import com.koala.mapper.UserMapper;
import com.koala.service.AfterSaleService;
import com.koala.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AfterSaleServiceImpl implements AfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final OrderService orderService;

    public AfterSaleServiceImpl(AfterSaleMapper afterSaleMapper, OrderMapper orderMapper,
                                UserMapper userMapper, OrderService orderService) {
        this.afterSaleMapper = afterSaleMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.orderService = orderService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String apply(Long userId, AfterSaleApplyRequest req) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, req.getOrderNo()));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        // 待发货→仅退款(1) / 待收货→退货退款(2)；其余状态走客服人工承接
        int type;
        if (order.getStatus() == 1) {
            type = 1;
        } else if (order.getStatus() == 2) {
            type = 2;
        } else {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "当前订单状态不可申请售后");
        }
        // 幂等：同订单存在进行中的售后单(非终态4/5)则拦截
        Long active = afterSaleMapper.selectCount(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getOrderNo, req.getOrderNo())
                .notIn(AfterSale::getStatus, 4, 5));
        if (active != null && active > 0) {
            throw new BizException(ErrorCode.DUPLICATE_SUBMIT.getCode(), "该订单已有进行中的售后单");
        }
        if (req.getEvidenceImages() != null && req.getEvidenceImages().size() > 3) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "凭证图最多3张");
        }

        String afterSaleNo = genAfterSaleNo();
        AfterSale as = new AfterSale();
        as.setAfterSaleNo(afterSaleNo);
        as.setOrderNo(req.getOrderNo());
        as.setUserId(userId);
        as.setType(type);
        as.setReason(req.getReason());
        as.setRemark(req.getRemark() != null ? req.getRemark() : "");
        as.setEvidenceImages(req.getEvidenceImages() != null && !req.getEvidenceImages().isEmpty()
                ? JSONUtil.toJsonStr(req.getEvidenceImages()) : null);
        as.setRefundAmount(order.getPayAmount());
        as.setReturnTrackingNo("");
        as.setStatus(0);
        as.setAuditRemark("");
        afterSaleMapper.insert(as);

        // order → 5售后中 (CAS from current status)
        int affected = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, 5)
                .eq(Order::getOrderNo, req.getOrderNo())
                .eq(Order::getStatus, order.getStatus()));
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单状态已变更，请重试");
        }
        return afterSaleNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByUser(Long userId, String afterSaleNo) {
        AfterSale as = requireOwned(userId, afterSaleNo);
        if (as.getStatus() != 0) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅待审核的售后单可撤销");
        }
        closeAndRestoreOrder(as, "");
    }

    @Override
    public void fillTracking(Long userId, AfterSaleTrackingRequest req) {
        AfterSale as = requireOwned(userId, req.getAfterSaleNo());
        if (as.getType() == null || as.getType() != 2) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅退货退款需填写寄回单号");
        }
        // 待寄回(1)可填；待商家收货(2)可修改，以最新为准
        if (as.getStatus() != 1 && as.getStatus() != 2) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "当前状态不可填写寄回单号");
        }
        afterSaleMapper.update(null, Wrappers.<AfterSale>lambdaUpdate()
                .set(AfterSale::getReturnTrackingNo, req.getReturnTrackingNo())
                .set(AfterSale::getStatus, 2)
                .eq(AfterSale::getAfterSaleNo, req.getAfterSaleNo())
                .in(AfterSale::getStatus, 1, 2));
    }

    @Override
    public PageResult<AfterSaleView> myList(Long userId, Integer status, long page, long size) {
        IPage<AfterSale> p = afterSaleMapper.selectPage(new Page<>(page, size),
                Wrappers.<AfterSale>lambdaQuery()
                        .eq(AfterSale::getUserId, userId)
                        .eq(status != null, AfterSale::getStatus, status)
                        .orderByDesc(AfterSale::getId));
        List<AfterSaleView> list = p.getRecords().stream().map(this::toView).collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public AfterSaleView detail(Long userId, String afterSaleNo) {
        return toView(requireOwned(userId, afterSaleNo));
    }

    // ---- 后台 ----

    @Override
    public PageResult<AdminAfterSaleView> adminList(String keyword, Integer status, long page, long size) {
        final String kw = keyword != null ? keyword.trim() : null;
        Set<Long> matchedUserIds = Collections.emptySet();
        if (kw != null && !kw.isEmpty()) {
            matchedUserIds = userMapper.selectList(Wrappers.<User>lambdaQuery()
                            .like(User::getNickname, kw).select(User::getId))
                    .stream().map(User::getId).collect(Collectors.toSet());
        }
        final Set<Long> uids = matchedUserIds;
        IPage<AfterSale> p = afterSaleMapper.selectPage(new Page<>(page, size),
                Wrappers.<AfterSale>lambdaQuery()
                        .eq(status != null, AfterSale::getStatus, status)
                        .and(kw != null && !kw.isEmpty(), w -> {
                            w.like(AfterSale::getAfterSaleNo, kw).or().like(AfterSale::getOrderNo, kw);
                            if (!uids.isEmpty()) {
                                w.or().in(AfterSale::getUserId, uids);
                            }
                        })
                        .orderByDesc(AfterSale::getId));
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        Map<Long, String> nickMap = userMapper.selectBatchIds(
                        p.getRecords().stream().map(AfterSale::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, User::getNickname));
        List<AdminAfterSaleView> list = p.getRecords().stream()
                .map(as -> toAdminView(as, nickMap.get(as.getUserId())))
                .collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public AdminAfterSaleView adminDetail(String afterSaleNo) {
        AfterSale as = afterSaleMapper.selectOne(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getAfterSaleNo, afterSaleNo));
        if (as == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        User user = userMapper.selectById(as.getUserId());
        return toAdminView(as, user != null ? user.getNickname() : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(AfterSaleAuditRequest req) {
        AfterSale as = afterSaleMapper.selectOne(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getAfterSaleNo, req.getAfterSaleNo()));
        if (as == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (as.getStatus() != 0) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅待审核的售后单可审核");
        }
        String remark = req.getAuditRemark() != null ? req.getAuditRemark() : "";
        if (!Boolean.TRUE.equals(req.getApproved())) {
            // 拒绝：售后单→5关闭，order 5→原状态
            if (StrUtil.isBlank(remark)) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "拒绝须填写理由");
            }
            closeAndRestoreOrder(as, remark);
            return;
        }
        // 同意
        if (as.getType() == 1) {
            // 仅退款：审核通过即打款 → 售后单 4已退款
            orderService.refundForAfterSale(as.getOrderNo(), as.getReason());
            afterSaleMapper.update(null, Wrappers.<AfterSale>lambdaUpdate()
                    .set(AfterSale::getStatus, 4)
                    .set(AfterSale::getAuditRemark, remark)
                    .eq(AfterSale::getAfterSaleNo, req.getAfterSaleNo())
                    .eq(AfterSale::getStatus, 0));
        } else {
            // 退货退款：→1待寄回，order 保持 5售后中
            afterSaleMapper.update(null, Wrappers.<AfterSale>lambdaUpdate()
                    .set(AfterSale::getStatus, 1)
                    .set(AfterSale::getAuditRemark, remark)
                    .eq(AfterSale::getAfterSaleNo, req.getAfterSaleNo())
                    .eq(AfterSale::getStatus, 0));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String afterSaleNo) {
        AfterSale as = afterSaleMapper.selectOne(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getAfterSaleNo, afterSaleNo));
        if (as == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (as.getType() == null || as.getType() != 2) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅退货退款需确认收货");
        }
        if (as.getStatus() != 2) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅买家已寄回的售后单可确认收货");
        }
        // 2买家已寄回 → 3商家已收货 → 退款 → 4已退款
        int affected = afterSaleMapper.update(null, Wrappers.<AfterSale>lambdaUpdate()
                .set(AfterSale::getStatus, 3)
                .eq(AfterSale::getAfterSaleNo, afterSaleNo)
                .eq(AfterSale::getStatus, 2));
        if (affected == 0) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR);
        }
        orderService.refundForAfterSale(as.getOrderNo(), as.getReason());
        afterSaleMapper.update(null, Wrappers.<AfterSale>lambdaUpdate()
                .set(AfterSale::getStatus, 4)
                .eq(AfterSale::getAfterSaleNo, afterSaleNo)
                .eq(AfterSale::getStatus, 3));
    }

    // ---- helpers ----

    /** 关闭售后单(→5)并把订单从 5售后中 回退到原状态(仅退款→1/退货退款→2)。 */
    private void closeAndRestoreOrder(AfterSale as, String auditRemark) {
        afterSaleMapper.update(null, Wrappers.<AfterSale>lambdaUpdate()
                .set(AfterSale::getStatus, 5)
                .set(StrUtil.isNotBlank(auditRemark), AfterSale::getAuditRemark, auditRemark)
                .eq(AfterSale::getAfterSaleNo, as.getAfterSaleNo())
                .eq(AfterSale::getStatus, as.getStatus()));
        int restore = as.getType() != null && as.getType() == 1 ? 1 : 2;
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, restore)
                .eq(Order::getOrderNo, as.getOrderNo())
                .eq(Order::getStatus, 5));
    }

    private AfterSale requireOwned(Long userId, String afterSaleNo) {
        AfterSale as = afterSaleMapper.selectOne(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getAfterSaleNo, afterSaleNo));
        if (as == null || !as.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return as;
    }

    private AfterSaleView toView(AfterSale as) {
        AfterSaleView v = new AfterSaleView();
        v.setAfterSaleNo(as.getAfterSaleNo());
        v.setOrderNo(as.getOrderNo());
        v.setType(as.getType());
        v.setReason(as.getReason());
        v.setRemark(as.getRemark());
        v.setEvidenceImages(parseImages(as.getEvidenceImages()));
        v.setRefundAmount(as.getRefundAmount());
        v.setReturnTrackingNo(as.getReturnTrackingNo());
        v.setStatus(as.getStatus());
        v.setAuditRemark(as.getAuditRemark());
        v.setCreatedAt(as.getCreatedAt());
        v.setUpdatedAt(as.getUpdatedAt());
        return v;
    }

    private AdminAfterSaleView toAdminView(AfterSale as, String nickname) {
        AdminAfterSaleView v = new AdminAfterSaleView();
        v.setAfterSaleNo(as.getAfterSaleNo());
        v.setOrderNo(as.getOrderNo());
        v.setUserId(as.getUserId());
        v.setNickname(nickname);
        v.setType(as.getType());
        v.setReason(as.getReason());
        v.setRemark(as.getRemark());
        v.setEvidenceImages(parseImages(as.getEvidenceImages()));
        v.setRefundAmount(as.getRefundAmount());
        v.setReturnTrackingNo(as.getReturnTrackingNo());
        v.setStatus(as.getStatus());
        v.setAuditRemark(as.getAuditRemark());
        v.setCreatedAt(as.getCreatedAt());
        v.setUpdatedAt(as.getUpdatedAt());
        return v;
    }

    private List<String> parseImages(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(json, String.class);
    }

    private String genAfterSaleNo() {
        return "AS" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6);
    }
}
