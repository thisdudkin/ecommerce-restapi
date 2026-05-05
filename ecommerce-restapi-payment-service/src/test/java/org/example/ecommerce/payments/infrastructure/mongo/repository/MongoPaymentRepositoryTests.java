package org.example.ecommerce.payments.infrastructure.mongo.repository;

import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.infrastructure.mapper.PaymentMapper;
import org.example.ecommerce.payments.infrastructure.mongo.document.PaymentDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoPaymentRepositoryTests {

    @Mock
    private PaymentDocumentRepository paymentDocumentRepository;

    @Captor
    private ArgumentCaptor<PaymentDocument> paymentDocumentCaptor;

    @Captor
    private ArgumentCaptor<List<PaymentDocument>> paymentDocumentsCaptor;

    private MongoPaymentRepository repository;

    @BeforeEach
    void setUp() {
        PaymentMapper mapper = Mappers.getMapper(PaymentMapper.class);
        repository = new MongoPaymentRepository(mapper, paymentDocumentRepository);
    }

    @Test
    void create_shouldSaveNewDocumentAndMapSavedEntity() {
        NewPayment newPayment = NewPayment.builder()
            .orderId(10L)
            .userId(20L)
            .status(PaymentStatus.SUCCESS)
            .timestamp(Instant.parse("2026-04-20T10:00:00Z"))
            .amount(new BigDecimal("15.50"))
            .build();

        when(paymentDocumentRepository.save(any(PaymentDocument.class))).thenAnswer(invocation -> {
            PaymentDocument input = invocation.getArgument(0);
            return new PaymentDocument(
                "payment-1",
                input.orderId(),
                input.userId(),
                input.status(),
                input.timestamp(),
                input.amount(),
                input.eventId(),
                input.publishedAt()
            );
        });

        Payment actual = repository.create(newPayment);

        verify(paymentDocumentRepository).save(paymentDocumentCaptor.capture());
        PaymentDocument saved = paymentDocumentCaptor.getValue();

        assertThat(saved.id()).isNull();
        assertThat(saved.orderId()).isEqualTo(10L);
        assertThat(saved.userId()).isEqualTo(20L);
        assertThat(saved.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(saved.timestamp()).isEqualTo(Instant.parse("2026-04-20T10:00:00Z"));
        assertThat(saved.amount()).isEqualByComparingTo("15.50");
        assertThat(saved.eventId()).isNotBlank();
        assertThat(saved.publishedAt()).isNull();

        assertThat(actual.id()).isEqualTo("payment-1");
        assertThat(actual.orderId()).isEqualTo(10L);
        assertThat(actual.userId()).isEqualTo(20L);
        assertThat(actual.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(actual.amount()).isEqualByComparingTo("15.50");
    }

    @Test
    void findByUserId_shouldMapDocumentsToDomain() {
        when(paymentDocumentRepository.findAllByUserIdOrderByTimestampDesc(7L)).thenReturn(List.of(
            new PaymentDocument("p-1", 1L, 7L, PaymentStatus.SUCCESS, Instant.parse("2026-04-20T10:00:00Z"), new BigDecimal("11.00"), "e-1", null)
        ));

        List<Payment> actual = repository.findByUserId(7L);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).id()).isEqualTo("p-1");
        assertThat(actual.get(0).userId()).isEqualTo(7L);
    }

    @Test
    void findByOrderId_shouldMapDocumentsToDomain() {
        when(paymentDocumentRepository.findAllByOrderIdOrderByTimestampDesc(9L)).thenReturn(List.of(
            new PaymentDocument("p-2", 9L, 70L, PaymentStatus.FAILED, Instant.parse("2026-04-20T11:00:00Z"), new BigDecimal("22.00"), "e-2", null)
        ));

        List<Payment> actual = repository.findByOrderId(9L);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).orderId()).isEqualTo(9L);
    }

    @Test
    void findByStatus_shouldMapDocumentsToDomain() {
        when(paymentDocumentRepository.findAllByStatusOrderByTimestampDesc(PaymentStatus.SUCCESS)).thenReturn(List.of(
            new PaymentDocument("p-3", 3L, 30L, PaymentStatus.SUCCESS, Instant.parse("2026-04-20T12:00:00Z"), new BigDecimal("33.00"), "e-3", null)
        ));

        List<Payment> actual = repository.findByStatus(PaymentStatus.SUCCESS);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void existsSuccessfulByOrderId_shouldDelegateToDocumentRepository() {
        when(paymentDocumentRepository.existsByOrderIdAndStatus(123L, PaymentStatus.SUCCESS)).thenReturn(true);

        boolean actual = repository.existsSuccessfulByOrderId(123L);

        assertThat(actual).isTrue();
    }

    @Test
    void getTotalSumByDateRangeForUser_shouldReturnAggregatedTotal() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-01T00:00:00Z");
        PaymentDocumentRepository.SumView sumView = () -> new BigDecimal("44.44");

        when(paymentDocumentRepository.sumByUserIdAndTimestampBetween(5L, from, to)).thenReturn(sumView);

        BigDecimal actual = repository.getTotalSumByDateRange(5L, from, to);

        assertThat(actual).isEqualByComparingTo("44.44");
    }

    @Test
    void getTotalSumByDateRangeForUser_shouldReturnZeroWhenAggregateIsMissing() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-01T00:00:00Z");

        when(paymentDocumentRepository.sumByUserIdAndTimestampBetween(5L, from, to)).thenReturn(null);

        BigDecimal actual = repository.getTotalSumByDateRange(5L, from, to);

        assertThat(actual).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalSumByDateRangeForAllUsers_shouldReturnZeroWhenTotalIsNull() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-01T00:00:00Z");
        PaymentDocumentRepository.SumView sumView = () -> null;

        when(paymentDocumentRepository.sumAllByTimestampBetween(from, to)).thenReturn(sumView);

        BigDecimal actual = repository.getTotalSumByDateRange(from, to);

        assertThat(actual).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findPendingPublicationBatch_shouldUsePageRequestAndMapResults() {
        when(paymentDocumentRepository.findPendingPublicationBatch(PageRequest.of(0, 3))).thenReturn(List.of(
            new PaymentDocument("p-10", 101L, 201L, PaymentStatus.SUCCESS, Instant.parse("2026-04-20T08:00:00Z"), new BigDecimal("55.55"), "event-10", null)
        ));

        List<PaymentOutboxRecord> actual = repository.findPendingPublicationBatch(3);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).paymentId()).isEqualTo("p-10");
        assertThat(actual.get(0).eventId()).isEqualTo("event-10");
        assertThat(actual.get(0).occurredAt()).isEqualTo(Instant.parse("2026-04-20T08:00:00Z"));
    }

    @Test
    void markPublished_shouldReturnImmediatelyWhenIdsAreNull() {
        repository.markPublished(null, Instant.parse("2026-04-20T14:00:00Z"));

        verify(paymentDocumentRepository, never()).findAllByIdInAndPublishedAtIsNull(any());
        verify(paymentDocumentRepository, never()).saveAll(any());
    }

    @Test
    void markPublished_shouldReturnImmediatelyWhenIdsAreEmpty() {
        repository.markPublished(List.of(), Instant.parse("2026-04-20T14:00:00Z"));

        verify(paymentDocumentRepository, never()).findAllByIdInAndPublishedAtIsNull(any());
        verify(paymentDocumentRepository, never()).saveAll(any());
    }

    @Test
    void markPublished_shouldReturnWhenNoDocumentsNeedUpdate() {
        Instant publishedAt = Instant.parse("2026-04-20T14:00:00Z");
        when(paymentDocumentRepository.findAllByIdInAndPublishedAtIsNull(List.of("p-1"))).thenReturn(List.of());

        repository.markPublished(List.of("p-1"), publishedAt);

        verify(paymentDocumentRepository).findAllByIdInAndPublishedAtIsNull(List.of("p-1"));
        verify(paymentDocumentRepository, never()).saveAll(any());
    }

    @Test
    void markPublished_shouldPersistUpdatedPublishedAtValue() {
        Instant publishedAt = Instant.parse("2026-04-20T15:00:00Z");
        PaymentDocument first = new PaymentDocument(
            "p-1",
            1L,
            11L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T10:00:00Z"),
            new BigDecimal("5.00"),
            "e-1",
            null
        );
        PaymentDocument second = new PaymentDocument(
            "p-2",
            2L,
            22L,
            PaymentStatus.FAILED,
            Instant.parse("2026-04-20T11:00:00Z"),
            new BigDecimal("6.00"),
            "e-2",
            null
        );

        when(paymentDocumentRepository.findAllByIdInAndPublishedAtIsNull(List.of("p-1", "p-2")))
            .thenReturn(List.of(first, second));

        repository.markPublished(List.of("p-1", "p-2"), publishedAt);

        verify(paymentDocumentRepository).saveAll(paymentDocumentsCaptor.capture());
        List<PaymentDocument> saved = paymentDocumentsCaptor.getValue();

        assertThat(saved)
            .hasSize(2)
            .satisfiesExactly(
                firstSaved -> {
                    assertThat(firstSaved.id()).isEqualTo("p-1");
                    assertThat(firstSaved.publishedAt()).isEqualTo(publishedAt);
                },
                secondSaved -> {
                    assertThat(secondSaved.id()).isEqualTo("p-2");
                    assertThat(secondSaved.publishedAt()).isEqualTo(publishedAt);
                }
            );
    }

}
