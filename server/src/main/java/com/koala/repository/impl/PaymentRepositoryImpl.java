package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.Payment;
import com.koala.enums.PaymentStatus;
import com.koala.mapper.PaymentMapper;
import com.koala.repository.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentMapper paymentMapper;

    public PaymentRepositoryImpl(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    @Override
    public void insert(Payment payment) {
        paymentMapper.insert(payment);
    }

    @Override
    public Payment findByOrderNoAndStatus(String orderNo, int status) {
        return paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderNo, orderNo)
                .eq(Payment::getStatus, status));
    }

    @Override
    public int markSuccess(String orderNo, String transactionId, LocalDateTime paidAt) {
        // uk_transaction 是 UNIQUE(transaction_id)：MySQL 允许多个 NULL 但空串会冲突，统一入库前空串转 null。
        String normalizedTx = (transactionId == null || transactionId.isEmpty()) ? null : transactionId;
        return paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .set(Payment::getStatus, PaymentStatus.SUCCESS.code())
                .set(Payment::getTransactionId, normalizedTx)
                .set(Payment::getPaidAt, paidAt)
                .eq(Payment::getOrderNo, orderNo)
                .eq(Payment::getStatus, PaymentStatus.PENDING.code()));
    }

    @Override
    public void markRefunded(Long paymentId) {
        paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .set(Payment::getStatus, PaymentStatus.REFUNDED.code())
                .eq(Payment::getId, paymentId));
    }
}
