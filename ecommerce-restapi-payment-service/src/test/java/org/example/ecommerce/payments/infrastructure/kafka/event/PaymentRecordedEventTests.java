package org.example.ecommerce.payments.infrastructure.kafka.event;

import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRecordedEventTests {

    @Test
    void from_shouldMapOutboxRecordToKafkaEvent() {
        PaymentOutboxRecord outboxRecord = new PaymentOutboxRecord(
            "payment-1",
            "event-1",
            101L,
            202L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T17:00:00Z"),
            new BigDecimal("88.88")
        );

        PaymentRecordedEvent actual = PaymentRecordedEvent.from(outboxRecord);

        assertThat(actual.eventId()).isEqualTo("event-1");
        assertThat(actual.eventType()).isEqualTo(PaymentRecordedEvent.EVENT_TYPE);
        assertThat(actual.occurredAt()).isEqualTo(Instant.parse("2026-04-20T17:00:00Z"));
        assertThat(actual.paymentId()).isEqualTo("payment-1");
        assertThat(actual.orderId()).isEqualTo(101L);
        assertThat(actual.userId()).isEqualTo(202L);
        assertThat(actual.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(actual.amount()).isEqualByComparingTo("88.88");
    }
}
