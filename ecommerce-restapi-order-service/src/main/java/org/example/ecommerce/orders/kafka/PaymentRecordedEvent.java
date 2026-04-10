package org.example.ecommerce.orders.kafka;

import org.example.ecommerce.orders.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRecordedEvent(
    String eventId,
    String eventType,
    Instant occurredAt,
    String paymentId,
    Long orderId,
    Long userId,
    PaymentStatus status,
    BigDecimal amount
) { }
