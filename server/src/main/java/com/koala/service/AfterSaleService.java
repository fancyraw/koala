package com.koala.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koala.common.exception.BizException;
import com.koala.common.util.SerialNoGenerator;
import com.koala.converter.AfterSaleConverter;
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
import com.koala.enums.AfterSaleStatus;
import com.koala.enums.AfterSaleType;
import com.koala.enums.OrderStatus;
import com.koala.repository.AfterSaleRepository;
import com.koala.repository.OrderRepository;
import com.koala.repository.UserRepository;
import com.koala.service.order.OrderRefundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AfterSaleService {

    private final AfterSaleRepository afterSaleRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderRefundService orderRefundService;

    public AfterSaleService(AfterSaleRepository afterSaleRepository, OrderRepository orderRepository,
                                UserRepository userRepository, OrderRefundService orderRefundService) {
        this.afterSaleRepository = afterSaleRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderRefundService = orderRefundService;
    }

    @Transactional(rollbackFor = Exception.class)
    public String apply(Long userId, AfterSaleApplyRequest req) {
        Order order = orderRepository.findByNo(req.getOrderNo());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        // 待发货→仅退款 / 待收货→退货退款；其余状态走客服人工承接
        AfterSaleType type;
        if (OrderStatus.WAIT_SHIP.is(order.getStatus())) {
            type = AfterSaleType.REFUND_ONLY;
        } else if (OrderStatus.WAIT_RECEIVE.is(order.getStatus())) {
            type = AfterSaleType.RETURN_REFUND;
        } else {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "当前订单状态不可申请售后");
        }
        // 幂等：同订单存在进行中的售后单则拦截
        if (afterSaleRepository.countActiveByOrderNo(req.getOrderNo()) > 0) {
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
        as.setType(type.code());
        as.setReason(req.getReason());
        as.setRemark(req.getRemark() != null ? req.getRemark() : "");
        as.setEvidenceImages(req.getEvidenceImages() != null && !req.getEvidenceImages().isEmpty()
                ? JSONUtil.toJsonStr(req.getEvidenceImages()) : null);
        as.setRefundAmount(order.getPayAmount());
        as.setReturnTrackingNo("");
        as.setStatus(AfterSaleStatus.PENDING_AUDIT.code());
        as.setAuditRemark("");
        afterSaleRepository.insert(as);

        // order → 售后中 (CAS from current status)
        int affected = orderRepository.updateStatusCas(req.getOrderNo(), order.getStatus(), OrderStatus.AFTER_SALE.code());
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单状态已变更，请重试");
        }
        return afterSaleNo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelByUser(Long userId, String afterSaleNo) {
        AfterSale as = requireOwned(userId, afterSaleNo);
        if (!AfterSaleStatus.PENDING_AUDIT.is(as.getStatus())) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅待审核的售后单可撤销");
        }
        closeAndRestoreOrder(as, "");
    }

    public void fillTracking(Long userId, AfterSaleTrackingRequest req) {
        AfterSale as = requireOwned(userId, req.getAfterSaleNo());
        if (!AfterSaleType.RETURN_REFUND.is(as.getType())) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅退货退款需填写寄回单号");
        }
        // 待寄回可填；已寄回可修改，以最新为准
        if (!AfterSaleStatus.APPROVED_WAIT_RETURN.is(as.getStatus())
                && !AfterSaleStatus.BUYER_RETURNED.is(as.getStatus())) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "当前状态不可填写寄回单号");
        }
        afterSaleRepository.updateTracking(req.getAfterSaleNo(), req.getReturnTrackingNo(),
                AfterSaleStatus.BUYER_RETURNED.code(),
                Arrays.asList(AfterSaleStatus.APPROVED_WAIT_RETURN.code(), AfterSaleStatus.BUYER_RETURNED.code()));
    }

    public PageResult<AfterSaleView> myList(Long userId, Integer status, long page, long size) {
        IPage<AfterSale> p = afterSaleRepository.pageByUser(userId, status, page, size);
        List<AfterSaleView> list = p.getRecords().stream().map(AfterSaleConverter::toView).collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public AfterSaleView detail(Long userId, String afterSaleNo) {
        return AfterSaleConverter.toView(requireOwned(userId, afterSaleNo));
    }

    // ---- 后台 ----

    public PageResult<AdminAfterSaleView> adminList(String keyword, Integer status, long page, long size) {
        final String kw = keyword != null ? keyword.trim() : null;
        Set<Long> matchedUserIds = Collections.emptySet();
        if (StrUtil.isNotBlank(kw)) {
            matchedUserIds = new java.util.HashSet<>(userRepository.findIdsByNicknameLike(kw));
        }
        IPage<AfterSale> p = afterSaleRepository.pageForAdmin(kw, matchedUserIds, status, page, size);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        Set<Long> uids = p.getRecords().stream().map(AfterSale::getUserId).collect(Collectors.toSet());
        Map<Long, String> nickMap = userRepository.findByIds(uids).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
        List<AdminAfterSaleView> list = p.getRecords().stream()
                .map(as -> AfterSaleConverter.toAdminView(as, nickMap.get(as.getUserId())))
                .collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public AdminAfterSaleView adminDetail(String afterSaleNo) {
        AfterSale as = afterSaleRepository.findByNo(afterSaleNo);
        if (as == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        User user = userRepository.findById(as.getUserId());
        return AfterSaleConverter.toAdminView(as, user != null ? user.getNickname() : null);
    }

    public void audit(AfterSaleAuditRequest req) {
        AfterSale as = afterSaleRepository.findByNo(req.getAfterSaleNo());
        if (as == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!AfterSaleStatus.PENDING_AUDIT.is(as.getStatus())) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅待审核的售后单可审核");
        }
        String remark = req.getAuditRemark() != null ? req.getAuditRemark() : "";
        if (!Boolean.TRUE.equals(req.getApproved())) {
            // 拒绝：售后单关闭，order 从售后中回退到原状态
            if (StrUtil.isBlank(remark)) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "拒绝须填写理由");
            }
            closeAndRestoreOrder(as, remark);
            return;
        }
        // 同意
        if (AfterSaleType.REFUND_ONLY.is(as.getType())) {
            // 仅退款：先 CAS 拿到独占权，再调外部渠道；CAS 返回 0 说明有其他线程已在处理。
            int affected = afterSaleRepository.updateStatusCas(req.getAfterSaleNo(),
                    AfterSaleStatus.PENDING_AUDIT.code(), AfterSaleStatus.MERCHANT_RECEIVED.code(), remark);
            if (affected == 0) {
                throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "售后单状态已变更，请刷新重试");
            }
            try {
                orderRefundService.refundForAfterSale(as.getOrderNo(), as.getReason());
            } catch (RuntimeException e) {
                // 渠道失败：CAS 回退到 PENDING_AUDIT，人工重试。
                afterSaleRepository.updateStatusCas(req.getAfterSaleNo(),
                        AfterSaleStatus.MERCHANT_RECEIVED.code(), AfterSaleStatus.PENDING_AUDIT.code(), remark);
                throw e;
            }
            afterSaleRepository.updateStatusCas(req.getAfterSaleNo(),
                    AfterSaleStatus.MERCHANT_RECEIVED.code(), AfterSaleStatus.REFUNDED.code(), remark);
        } else {
            // 退货退款：→待寄回，order 保持售后中
            afterSaleRepository.updateStatusCas(req.getAfterSaleNo(),
                    AfterSaleStatus.PENDING_AUDIT.code(), AfterSaleStatus.APPROVED_WAIT_RETURN.code(), remark);
        }
    }

    public void confirmReceive(String afterSaleNo) {
        AfterSale as = afterSaleRepository.findByNo(afterSaleNo);
        if (as == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!AfterSaleType.RETURN_REFUND.is(as.getType())) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅退货退款需确认收货");
        }
        if (!AfterSaleStatus.BUYER_RETURNED.is(as.getStatus())) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode(), "仅买家已寄回的售后单可确认收货");
        }
        // 买家已寄回 → 商家已收货：CAS 拿独占权，防止并发触发两次退款。
        int affected = afterSaleRepository.updateStatusCas(afterSaleNo,
                AfterSaleStatus.BUYER_RETURNED.code(), AfterSaleStatus.MERCHANT_RECEIVED.code(), null);
        if (affected == 0) {
            throw new BizException(ErrorCode.AFTER_SALE_STATUS_ERROR);
        }
        try {
            orderRefundService.refundForAfterSale(as.getOrderNo(), as.getReason());
        } catch (RuntimeException e) {
            // 渠道失败：售后单退回 BUYER_RETURNED 以便重试。
            afterSaleRepository.updateStatusCas(afterSaleNo,
                    AfterSaleStatus.MERCHANT_RECEIVED.code(), AfterSaleStatus.BUYER_RETURNED.code(), null);
            throw e;
        }
        afterSaleRepository.updateStatusCas(afterSaleNo,
                AfterSaleStatus.MERCHANT_RECEIVED.code(), AfterSaleStatus.REFUNDED.code(), null);
    }

    // ---- helpers ----

    /** 关闭售后单(拒绝)并把订单从售后中回退到原状态(仅退款→待发货/退货退款→待收货)。 */
    private void closeAndRestoreOrder(AfterSale as, String auditRemark) {
        afterSaleRepository.updateStatusCas(as.getAfterSaleNo(), as.getStatus(),
                AfterSaleStatus.REJECTED.code(), StrUtil.isNotBlank(auditRemark) ? auditRemark : null);
        int restore = AfterSaleType.REFUND_ONLY.is(as.getType())
                ? OrderStatus.WAIT_SHIP.code() : OrderStatus.WAIT_RECEIVE.code();
        orderRepository.updateStatusCas(as.getOrderNo(), OrderStatus.AFTER_SALE.code(), restore);
    }

    private AfterSale requireOwned(Long userId, String afterSaleNo) {
        AfterSale as = afterSaleRepository.findByNo(afterSaleNo);
        if (as == null || !as.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return as;
    }

    private String genAfterSaleNo() {
        return SerialNoGenerator.next(SerialNoGenerator.AFTER_SALE_PREFIX);
    }
}
