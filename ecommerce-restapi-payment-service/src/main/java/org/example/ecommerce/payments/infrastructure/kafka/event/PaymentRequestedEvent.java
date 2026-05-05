package org.example.ecommerce.payments.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRequestedEvent(
    String eventId,
    String eventType,
    Instant occurredAt,
    Long orderId,
    Long userId,
    BigDecimal amount
) {
    public static final String EVENT_TYPE = "payment.requested.v1";
}
