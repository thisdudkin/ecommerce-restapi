package org.example.ecommerce.payments.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.domain.repository.PaymentRepository;
import org.example.ecommerce.payments.infrastructure.external.adapter.RandomNumberProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RandomNumberProvider randomNumberProvider;
    private final PaymentRepository paymentRepository;

    public void create(NewPayment newPayment) {
        log.info(
            "Creating payment: Order ID: {}, User ID: {}, Amount: {}",
            newPayment.orderId(),
            newPayment.userId(),
            newPayment.amount()
        );

        PaymentStatus status = resolveStatus();
        NewPayment entity = newPayment.toBuilder()
            .status(status)
            .timestamp(Instant.now())
            .build();

        Payment savedEntity = paymentRepository.create(entity);

        log.info(
            "Payment created: Payment ID: {}, Order ID: {}, User ID: {}, Status: {}",
            savedEntity.id(),
            savedEntity.orderId(),
            savedEntity.userId(),
            savedEntity.status()
        );
    }

    public List<Payment> getByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    public List<Payment> getByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> getByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    public BigDecimal getTotalSumByDateRange(Long userId, Instant from, Instant to) {
        return paymentRepository.getTotalSumByDateRange(userId, from, to);
    }

    public BigDecimal getTotalSumByDateRange(Instant from, Instant to) {
        return paymentRepository.getTotalSumByDateRange(from, to);
    }

    private PaymentStatus resolveStatus() {
        int randomNumber = randomNumberProvider.nextInt();
        PaymentStatus status = resolveStatus(randomNumber);

        log.debug("Resolved payment status: Number: {}, Status: {}", randomNumber, status);

        return status;
    }

    private PaymentStatus resolveStatus(int randomNumber) {
        return randomNumber % 2 == 0
            ? PaymentStatus.SUCCESS
            : PaymentStatus.FAILED;
    }

}
