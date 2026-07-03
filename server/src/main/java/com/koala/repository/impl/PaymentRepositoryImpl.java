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
        return paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .set(Payment::getStatus, PaymentStatus.SUCCESS.code())
                .set(Payment::getTransactionId, transactionId)
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
