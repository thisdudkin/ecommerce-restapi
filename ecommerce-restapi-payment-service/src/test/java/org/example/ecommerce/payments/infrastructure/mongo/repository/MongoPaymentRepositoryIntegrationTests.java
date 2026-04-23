package org.example.ecommerce.payments.infrastructure.mongo.repository;

import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.infrastructure.mapper.PaymentMapperImpl;
import org.example.ecommerce.payments.infrastructure.mongo.document.PaymentDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
@Import({
    MongoPaymentRepository.class,
    PaymentMapperImpl.class
})
class MongoPaymentRepositoryIntegrationTests {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MongoPaymentRepository repository;

    @Autowired
    private PaymentDocumentRepository paymentDocumentRepository;

    @BeforeEach
    void setUp() {
        paymentDocumentRepository.deleteAll();
    }

    @Test
    void createShouldPersistPaymentAndExposePendingOutboxRecord() {
        NewPayment newPayment = NewPayment.builder()
            .orderId(101L)
            .userId(202L)
            .status(PaymentStatus.SUCCESS)
            .timestamp(Instant.parse("2026-04-20T10:00:00Z"))
            .amount(new BigDecimal("55.50"))
            .build();

        Payment saved = repository.create(newPayment);
        List<PaymentOutboxRecord> outbox = repository.findPendingPublicationBatch(10);

        assertThat(saved.id()).isNotBlank();
        assertThat(saved.orderId()).isEqualTo(101L);
        assertThat(saved.userId()).isEqualTo(202L);
        assertThat(saved.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(saved.amount()).isEqualByComparingTo("55.50");

        assertThat(outbox).hasSize(1);
        assertThat(outbox.getFirst().paymentId()).isEqualTo(saved.id());
        assertThat(outbox.getFirst().orderId()).isEqualTo(101L);
        assertThat(outbox.getFirst().userId()).isEqualTo(202L);
        assertThat(outbox.getFirst().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(outbox.getFirst().amount()).isEqualByComparingTo("55.50");
        assertThat(outbox.getFirst().eventId()).isNotBlank();
    }

    @Test
    void getTotalSumByDateRangeShouldIgnoreFailedPaymentsAndMarkPublishedShouldSetPublishedAt() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-01T00:00:00Z");

        PaymentDocument success1 = paymentDocumentRepository.save(new PaymentDocument(
            "payment-1",
            1L,
            500L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-10T10:00:00Z"),
            new BigDecimal("10.00"),
            "event-1",
            null
        ));
        paymentDocumentRepository.save(new PaymentDocument(
            "payment-2",
            2L,
            500L,
            PaymentStatus.FAILED,
            Instant.parse("2026-04-11T10:00:00Z"),
            new BigDecimal("999.00"),
            "event-2",
            null
        ));
        PaymentDocument success2 = paymentDocumentRepository.save(new PaymentDocument(
            "payment-3",
            3L,
            500L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-12T10:00:00Z"),
            new BigDecimal("15.00"),
            "event-3",
            null
        ));

        BigDecimal total = repository.getTotalSumByDateRange(500L, from, to);
        repository.markPublished(List.of(success1.id(), success2.id()), Instant.parse("2026-04-20T15:00:00Z"));

        assertThat(total).isEqualByComparingTo("25.00");

        List<PaymentDocument> documents = paymentDocumentRepository.findAll();

        assertThat(documents)
            .filteredOn(document -> List.of("payment-1", "payment-3").contains(document.id()))
            .hasSize(2)
            .allSatisfy(document ->
                assertThat(document.publishedAt())
                    .isEqualTo(Instant.parse("2026-04-20T15:00:00Z")));
    }
}
