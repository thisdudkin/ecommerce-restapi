package org.example.ecommerce.payments.domain.repository;

import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PaymentRepository {
    Payment create(NewPayment newPayment);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsSuccessfulByOrderId(Long orderId);

    BigDecimal getTotalSumByDateRange(Long userId, Instant from, Instant to);

    BigDecimal getTotalSumByDateRange(Instant from, Instant to);

    List<PaymentOutboxRecord> findPendingPublicationBatch(int limit);

    void markPublished(Collection<String> paymentIds, Instant publishedAt);
}
