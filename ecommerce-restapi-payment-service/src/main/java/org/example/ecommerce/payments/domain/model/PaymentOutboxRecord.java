package org.example.ecommerce.payments.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentOutboxRecord(
    String paymentId,
    String eventId,
    Long orderId,
    Long userId,
    PaymentStatus status,
    Instant occurredAt,
    BigDecimal amount
) { }
