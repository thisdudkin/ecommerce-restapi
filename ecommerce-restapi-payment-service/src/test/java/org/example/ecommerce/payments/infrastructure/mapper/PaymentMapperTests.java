package org.example.ecommerce.payments.infrastructure.mapper;

import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.infrastructure.mongo.document.PaymentDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTests {

    private final PaymentMapper mapper = Mappers.getMapper(PaymentMapper.class);

    @Test
    void toDomain_shouldMapAllFields() {
        PaymentDocument document = new PaymentDocument(
            "payment-1",
            10L,
            20L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T09:00:00Z"),
            new BigDecimal("12.34"),
            "event-1",
            Instant.parse("2026-04-20T09:01:00Z")
        );

        Payment actual = mapper.toDomain(document);

        assertThat(actual.id()).isEqualTo("payment-1");
        assertThat(actual.orderId()).isEqualTo(10L);
        assertThat(actual.userId()).isEqualTo(20L);
        assertThat(actual.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(actual.timestamp()).isEqualTo(Instant.parse("2026-04-20T09:00:00Z"));
        assertThat(actual.amount()).isEqualByComparingTo("12.34");
    }

    @Test
    void toOutboxRecord_shouldMapIdToPaymentIdAndTimestampToOccurredAt() {
        PaymentDocument document = new PaymentDocument(
            "payment-2",
            30L,
            40L,
            PaymentStatus.FAILED,
            Instant.parse("2026-04-20T10:00:00Z"),
            new BigDecimal("99.99"),
            "event-2",
            null
        );

        PaymentOutboxRecord actual = mapper.toOutboxRecord(document);

        assertThat(actual.paymentId()).isEqualTo("payment-2");
        assertThat(actual.eventId()).isEqualTo("event-2");
        assertThat(actual.orderId()).isEqualTo(30L);
        assertThat(actual.userId()).isEqualTo(40L);
        assertThat(actual.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(actual.occurredAt()).isEqualTo(Instant.parse("2026-04-20T10:00:00Z"));
        assertThat(actual.amount()).isEqualByComparingTo("99.99");
    }
}
