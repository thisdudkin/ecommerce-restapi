package org.example.ecommerce.payments.infrastructure.kafka.publisher;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.example.ecommerce.payments.config.PaymentMessagingProperties;
import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.domain.repository.PaymentRepository;
import org.example.ecommerce.payments.infrastructure.kafka.event.PaymentRecordedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxPublisherTests {

    @Mock
    private PaymentRepository repository;

    @Mock
    private KafkaTemplate<String, PaymentRecordedEvent> kafkaTemplate;

    @Mock
    private PaymentMessagingProperties properties;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, PaymentRecordedEvent>> producerRecordCaptor;

    @Captor
    private ArgumentCaptor<Instant> instantCaptor;

    private PaymentOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new PaymentOutboxPublisher(repository, kafkaTemplate, properties);
    }

    @Test
    void publishPendingEvents_shouldReturnImmediatelyWhenNoPendingRecords() {
        when(properties.outboxBatchSize()).thenReturn(10);
        when(repository.findPendingPublicationBatch(10)).thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(repository).findPendingPublicationBatch(10);
        verifyNoInteractions(kafkaTemplate);
        verify(repository, never()).markPublished(any(), any());
    }

    @Test
    void publishPendingEvents_shouldMarkOnlySuccessfullyPublishedRecords() {
        PaymentOutboxRecord successful = new PaymentOutboxRecord(
            "payment-1",
            "event-1",
            11L,
            101L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T10:00:00Z"),
            new BigDecimal("19.99")
        );
        PaymentOutboxRecord failed = new PaymentOutboxRecord(
            "payment-2",
            "event-2",
            22L,
            202L,
            PaymentStatus.FAILED,
            Instant.parse("2026-04-20T11:00:00Z"),
            new BigDecimal("29.99")
        );

        when(properties.outboxBatchSize()).thenReturn(2);
        when(properties.recordedTopicName()).thenReturn("payments.recorded.v1");
        when(repository.findPendingPublicationBatch(2)).thenReturn(List.of(successful, failed));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
            .thenReturn(successfulFuture("payments.recorded.v1", successful))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

        publisher.publishPendingEvents();

        verify(kafkaTemplate, org.mockito.Mockito.times(2)).send(producerRecordCaptor.capture());
        List<ProducerRecord<String, PaymentRecordedEvent>> records = producerRecordCaptor.getAllValues();

        assertThat(records).hasSize(2);

        ProducerRecord<String, PaymentRecordedEvent> first = records.get(0);
        assertThat(first.topic()).isEqualTo("payments.recorded.v1");
        assertThat(first.key()).isEqualTo("11");
        assertThat(first.value().eventId()).isEqualTo("event-1");
        assertThat(first.value().eventType()).isEqualTo(PaymentRecordedEvent.EVENT_TYPE);
        assertThat(first.value().paymentId()).isEqualTo("payment-1");
        assertThat(first.value().orderId()).isEqualTo(11L);
        assertThat(first.value().userId()).isEqualTo(101L);
        assertThat(first.value().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(first.value().amount()).isEqualByComparingTo("19.99");
        assertThat(headerValue(first, "event-id")).isEqualTo("event-1");
        assertThat(headerValue(first, "event-type")).isEqualTo(PaymentRecordedEvent.EVENT_TYPE);

        verify(repository).markPublished(
            argThat(ids -> ids.size() == 1 && ids.contains("payment-1")),
            instantCaptor.capture()
        );
        assertThat(instantCaptor.getValue()).isNotNull();
    }

    @Test
    void publishPendingEvents_shouldNotMarkAnythingWhenAllPublicationsFail() {
        PaymentOutboxRecord failed = new PaymentOutboxRecord(
            "payment-9",
            "event-9",
            999L,
            555L,
            PaymentStatus.FAILED,
            Instant.parse("2026-04-20T12:00:00Z"),
            new BigDecimal("1.00")
        );

        when(properties.outboxBatchSize()).thenReturn(1);
        when(properties.recordedTopicName()).thenReturn("payments.recorded.v1");
        when(repository.findPendingPublicationBatch(1)).thenReturn(List.of(failed));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("send failed")));

        publisher.publishPendingEvents();

        verify(repository, never()).markPublished(any(), any());
    }

    private CompletableFuture<SendResult<String, PaymentRecordedEvent>> successfulFuture(
        String topic,
        PaymentOutboxRecord outboxRecord
    ) {
        ProducerRecord<String, PaymentRecordedEvent> record = new ProducerRecord<>(
            topic,
            outboxRecord.orderId().toString(),
            PaymentRecordedEvent.from(outboxRecord)
        );

        RecordMetadata metadata = new RecordMetadata(
            new TopicPartition(topic, 0),
            0L,
            0,
            0L,
            0,
            0
        );

        return CompletableFuture.completedFuture(new SendResult<>(record, metadata));
    }

    private String headerValue(ProducerRecord<String, PaymentRecordedEvent> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}
