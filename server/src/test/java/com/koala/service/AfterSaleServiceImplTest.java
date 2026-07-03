package com.koala.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.aftersale.AfterSaleApplyRequest;
import com.koala.dto.aftersale.AfterSaleAuditRequest;
import com.koala.dto.aftersale.AfterSaleTrackingRequest;
import com.koala.entity.AfterSale;
import com.koala.entity.Order;
import com.koala.entity.User;
import com.koala.enums.AfterSaleStatus;
import com.koala.enums.AfterSaleType;
import com.koala.enums.OrderStatus;
import com.koala.repository.AfterSaleRepository;
import com.koala.repository.OrderRepository;
import com.koala.repository.UserRepository;
import com.koala.service.impl.AfterSaleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfterSaleServiceImplTest {

    @Mock
    private AfterSaleRepository afterSaleRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderService orderService;

    @InjectMocks
    private AfterSaleServiceImpl service;

    private Order order(String no, Long userId, int status) {
        Order o = new Order();
        o.setOrderNo(no);
        o.setUserId(userId);
        o.setStatus(status);
        o.setPayAmount(new BigDecimal("99.00"));
        return o;
    }

    private AfterSale afterSale(String no, Long userId, int type, int status) {
        AfterSale as = new AfterSale();
        as.setAfterSaleNo(no);
        as.setOrderNo("O1");
        as.setUserId(userId);
        as.setType(type);
        as.setStatus(status);
        as.setReason("坏了");
        return as;
    }

    // ---- apply ----

    @Test
    void apply_waitShip_createsRefundOnly_andMovesOrderToAfterSale() {
        when(orderRepository.findByNo("O1")).thenReturn(order("O1", 10L, OrderStatus.WAIT_SHIP.code()));
        when(afterSaleRepository.countActiveByOrderNo("O1")).thenReturn(0L);
        when(orderRepository.updateStatusCas("O1", OrderStatus.WAIT_SHIP.code(), OrderStatus.AFTER_SALE.code()))
                .thenReturn(1);

        AfterSaleApplyRequest req = new AfterSaleApplyRequest();
        req.setOrderNo("O1");
        req.setReason("不想要了");

        String no = service.apply(10L, req);

        assertThat(no).startsWith("AS");
        ArgumentCaptor<AfterSale> captor = ArgumentCaptor.forClass(AfterSale.class);
        verify(afterSaleRepository).insert(captor.capture());
        AfterSale saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(AfterSaleType.REFUND_ONLY.code());
        assertThat(saved.getStatus()).isEqualTo(AfterSaleStatus.PENDING_AUDIT.code());
        assertThat(saved.getRefundAmount()).isEqualByComparingTo("99.00");
    }

    @Test
    void apply_waitReceive_createsReturnRefund() {
        when(orderRepository.findByNo("O1")).thenReturn(order("O1", 10L, OrderStatus.WAIT_RECEIVE.code()));
        when(afterSaleRepository.countActiveByOrderNo("O1")).thenReturn(0L);
        when(orderRepository.updateStatusCas(anyString(), anyInt(), anyInt())).thenReturn(1);

        AfterSaleApplyRequest req = new AfterSaleApplyRequest();
        req.setOrderNo("O1");

        service.apply(10L, req);

        ArgumentCaptor<AfterSale> captor = ArgumentCaptor.forClass(AfterSale.class);
        verify(afterSaleRepository).insert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(AfterSaleType.RETURN_REFUND.code());
    }

    @Test
    void apply_orderNotOwned_throwsDataNotFound() {
        when(orderRepository.findByNo("O1")).thenReturn(order("O1", 999L, OrderStatus.WAIT_SHIP.code()));
        AfterSaleApplyRequest req = new AfterSaleApplyRequest();
        req.setOrderNo("O1");

        assertThatThrownBy(() -> service.apply(10L, req))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
        verify(afterSaleRepository, never()).insert(any());
    }

    @Test
    void apply_completedOrder_rejectedByStatus() {
        when(orderRepository.findByNo("O1")).thenReturn(order("O1", 10L, OrderStatus.COMPLETED.code()));
        AfterSaleApplyRequest req = new AfterSaleApplyRequest();
        req.setOrderNo("O1");

        assertThatThrownBy(() -> service.apply(10L, req))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.ORDER_STATUS_ERROR.getCode()));
    }

    @Test
    void apply_duplicateActiveAfterSale_rejected() {
        when(orderRepository.findByNo("O1")).thenReturn(order("O1", 10L, OrderStatus.WAIT_SHIP.code()));
        when(afterSaleRepository.countActiveByOrderNo("O1")).thenReturn(1L);
        AfterSaleApplyRequest req = new AfterSaleApplyRequest();
        req.setOrderNo("O1");

        assertThatThrownBy(() -> service.apply(10L, req))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DUPLICATE_SUBMIT.getCode()));
    }

    @Test
    void apply_orderStatusChangedConcurrently_rollsBack() {
        when(orderRepository.findByNo("O1")).thenReturn(order("O1", 10L, OrderStatus.WAIT_SHIP.code()));
        when(afterSaleRepository.countActiveByOrderNo("O1")).thenReturn(0L);
        when(orderRepository.updateStatusCas(anyString(), anyInt(), anyInt())).thenReturn(0);
        AfterSaleApplyRequest req = new AfterSaleApplyRequest();
        req.setOrderNo("O1");

        assertThatThrownBy(() -> service.apply(10L, req))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.ORDER_STATUS_ERROR.getCode()));
    }

    // ---- audit ----

    @Test
    void audit_approveRefundOnly_refundsAndMarksRefunded() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.REFUND_ONLY.code(), AfterSaleStatus.PENDING_AUDIT.code()));
        AfterSaleAuditRequest req = new AfterSaleAuditRequest();
        req.setAfterSaleNo("AS1");
        req.setApproved(true);
        req.setAuditRemark("同意");

        service.audit(req);

        verify(orderService).refundForAfterSale("O1", "坏了");
        verify(afterSaleRepository).updateStatusCas("AS1",
                AfterSaleStatus.PENDING_AUDIT.code(), AfterSaleStatus.REFUNDED.code(), "同意");
    }

    @Test
    void audit_approveReturnRefund_movesToWaitReturn_noRefundYet() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.RETURN_REFUND.code(), AfterSaleStatus.PENDING_AUDIT.code()));
        AfterSaleAuditRequest req = new AfterSaleAuditRequest();
        req.setAfterSaleNo("AS1");
        req.setApproved(true);

        service.audit(req);

        verify(orderService, never()).refundForAfterSale(anyString(), anyString());
        verify(afterSaleRepository).updateStatusCas("AS1",
                AfterSaleStatus.PENDING_AUDIT.code(), AfterSaleStatus.APPROVED_WAIT_RETURN.code(), "");
    }

    @Test
    void audit_rejectWithoutRemark_rejected() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.REFUND_ONLY.code(), AfterSaleStatus.PENDING_AUDIT.code()));
        AfterSaleAuditRequest req = new AfterSaleAuditRequest();
        req.setAfterSaleNo("AS1");
        req.setApproved(false);
        req.setAuditRemark("  ");

        assertThatThrownBy(() -> service.audit(req))
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void audit_reject_closesAndRestoresOrder() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.RETURN_REFUND.code(), AfterSaleStatus.PENDING_AUDIT.code()));
        AfterSaleAuditRequest req = new AfterSaleAuditRequest();
        req.setAfterSaleNo("AS1");
        req.setApproved(false);
        req.setAuditRemark("凭证不足");

        service.audit(req);

        verify(afterSaleRepository).updateStatusCas("AS1",
                AfterSaleStatus.PENDING_AUDIT.code(), AfterSaleStatus.REJECTED.code(), "凭证不足");
        // 退货退款关闭后订单回退到待收货
        verify(orderRepository).updateStatusCas("O1",
                OrderStatus.AFTER_SALE.code(), OrderStatus.WAIT_RECEIVE.code());
    }

    @Test
    void audit_notPending_rejected() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.REFUND_ONLY.code(), AfterSaleStatus.REFUNDED.code()));
        AfterSaleAuditRequest req = new AfterSaleAuditRequest();
        req.setAfterSaleNo("AS1");
        req.setApproved(true);

        assertThatThrownBy(() -> service.audit(req))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode()));
    }

    // ---- confirmReceive ----

    @Test
    void confirmReceive_returnRefund_receivesThenRefunds() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.RETURN_REFUND.code(), AfterSaleStatus.BUYER_RETURNED.code()));
        when(afterSaleRepository.updateStatusCas("AS1",
                AfterSaleStatus.BUYER_RETURNED.code(), AfterSaleStatus.MERCHANT_RECEIVED.code(), null)).thenReturn(1);

        service.confirmReceive("AS1");

        verify(orderService).refundForAfterSale("O1", "坏了");
        verify(afterSaleRepository).updateStatusCas("AS1",
                AfterSaleStatus.MERCHANT_RECEIVED.code(), AfterSaleStatus.REFUNDED.code(), null);
    }

    @Test
    void confirmReceive_refundOnlyType_rejected() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.REFUND_ONLY.code(), AfterSaleStatus.BUYER_RETURNED.code()));

        assertThatThrownBy(() -> service.confirmReceive("AS1"))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode()));
        verify(orderService, never()).refundForAfterSale(anyString(), anyString());
    }

    @Test
    void confirmReceive_casFails_throwsAndNoRefund() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.RETURN_REFUND.code(), AfterSaleStatus.BUYER_RETURNED.code()));
        when(afterSaleRepository.updateStatusCas("AS1",
                AfterSaleStatus.BUYER_RETURNED.code(), AfterSaleStatus.MERCHANT_RECEIVED.code(), null)).thenReturn(0);

        assertThatThrownBy(() -> service.confirmReceive("AS1")).isInstanceOf(BizException.class);
        verify(orderService, never()).refundForAfterSale(anyString(), anyString());
    }

    // ---- cancelByUser ----

    @Test
    void cancelByUser_pending_closesAndRestores() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.REFUND_ONLY.code(), AfterSaleStatus.PENDING_AUDIT.code()));

        service.cancelByUser(10L, "AS1");

        verify(afterSaleRepository).updateStatusCas("AS1",
                AfterSaleStatus.PENDING_AUDIT.code(), AfterSaleStatus.REJECTED.code(), null);
        // 仅退款回退到待发货
        verify(orderRepository).updateStatusCas("O1",
                OrderStatus.AFTER_SALE.code(), OrderStatus.WAIT_SHIP.code());
    }

    @Test
    void cancelByUser_notOwned_dataNotFound() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 999L, AfterSaleType.REFUND_ONLY.code(), AfterSaleStatus.PENDING_AUDIT.code()));

        assertThatThrownBy(() -> service.cancelByUser(10L, "AS1"))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void cancelByUser_notPending_rejected() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.REFUND_ONLY.code(), AfterSaleStatus.APPROVED_WAIT_RETURN.code()));

        assertThatThrownBy(() -> service.cancelByUser(10L, "AS1"))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode()));
    }

    // ---- fillTracking ----

    @Test
    void fillTracking_returnRefundWaitReturn_updates() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.RETURN_REFUND.code(), AfterSaleStatus.APPROVED_WAIT_RETURN.code()));
        AfterSaleTrackingRequest req = new AfterSaleTrackingRequest();
        req.setAfterSaleNo("AS1");
        req.setReturnTrackingNo("SF123");

        service.fillTracking(10L, req);

        verify(afterSaleRepository).updateTracking(eq("AS1"), eq("SF123"),
                eq(AfterSaleStatus.BUYER_RETURNED.code()), any());
    }

    @Test
    void fillTracking_refundOnlyType_rejected() {
        when(afterSaleRepository.findByNo("AS1"))
                .thenReturn(afterSale("AS1", 10L, AfterSaleType.REFUND_ONLY.code(), AfterSaleStatus.APPROVED_WAIT_RETURN.code()));
        AfterSaleTrackingRequest req = new AfterSaleTrackingRequest();
        req.setAfterSaleNo("AS1");
        req.setReturnTrackingNo("SF123");

        assertThatThrownBy(() -> service.fillTracking(10L, req))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.AFTER_SALE_STATUS_ERROR.getCode()));
        verify(afterSaleRepository, never()).updateTracking(anyString(), anyString(), anyInt(), any());
    }

    // ---- adminDetail ----

    @Test
    void adminDetail_notFound_throws() {
        when(afterSaleRepository.findByNo("AS1")).thenReturn(null);
        assertThatThrownBy(() -> service.adminDetail("AS1"))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void adminDetail_resolvesNickname() {
        AfterSale as = afterSale("AS1", 10L, AfterSaleType.REFUND_ONLY.code(), AfterSaleStatus.PENDING_AUDIT.code());
        when(afterSaleRepository.findByNo("AS1")).thenReturn(as);
        User u = new User();
        u.setId(10L);
        u.setNickname("张三");
        when(userRepository.findById(10L)).thenReturn(u);

        assertThat(service.adminDetail("AS1").getNickname()).isEqualTo("张三");
    }

    // ---- adminList ----

    @Test
    void adminList_emptyPage_returnsEmpty() {
        Page<AfterSale> empty = new Page<>(1, 10);
        empty.setRecords(Collections.emptyList());
        empty.setTotal(0);
        when(afterSaleRepository.pageForAdmin(isNull(), any(), isNull(), anyLong(), anyLong())).thenReturn(empty);

        assertThat(service.adminList(null, null, 1, 10).getList()).isEmpty();
        verify(userRepository, never()).findByIds(any());
    }
}
