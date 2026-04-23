package org.example.ecommerce.payments.infrastructure.kafka.event;

import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.domain.model.PaymentStatus;

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
) {
    public static final String EVENT_TYPE = "payment.recorded.v1";

    public static PaymentRecordedEvent from(PaymentOutboxRecord outboxRecord) {
        return new PaymentRecordedEvent(
            outboxRecord.eventId(),
            EVENT_TYPE,
            outboxRecord.occurredAt(),
            outboxRecord.paymentId(),
            outboxRecord.orderId(),
            outboxRecord.userId(),
            outboxRecord.status(),
            outboxRecord.amount()
        );
    }
}
