package com.koala.repository;

import com.koala.entity.Payment;

import java.time.LocalDateTime;

public interface PaymentRepository {

    void insert(Payment payment);

    /** 查订单某状态的支付流水(如 SUCCESS),不存在返回 null。 */
    Payment findByOrderNoAndStatus(String orderNo, int status);

    /**
     * 回调置成功:待支付(0)→成功(1) 并写 transactionId/paidAt。返回受影响行数。
     */
    int markSuccess(String orderNo, String transactionId, LocalDateTime paidAt);

    /** 置为已退款(3)。 */
    void markRefunded(Long paymentId);
}
