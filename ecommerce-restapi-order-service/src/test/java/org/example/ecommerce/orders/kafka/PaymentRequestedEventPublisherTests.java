package org.example.ecommerce.orders.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
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
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRequestedEventPublisherTests {

    @Mock
    private KafkaTemplate<String, PaymentRequestedEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, PaymentRequestedEvent>> recordCaptor;

    private PaymentRequestedEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new PaymentRequestedEventPublisher(
            kafkaTemplate,
            "ecommerce.payments.requested.v1"
        );
    }

    @Test
    void publishShouldSendProducerRecordWithExpectedKeyValueAndHeaders() {
        PaymentRequestedEvent event = new PaymentRequestedEvent(
            "event-1",
            PaymentRequestedEvent.EVENT_TYPE,
            Instant.parse("2026-04-20T12:00:00Z"),
            101L,
            202L,
            new BigDecimal("49.90")
        );

        when(kafkaTemplate.send(any(ProducerRecord.class)))
            .thenReturn(successfulFuture(event));

        publisher.publish(event);

        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, PaymentRequestedEvent> record = recordCaptor.getValue();

        assertThat(record.topic()).isEqualTo("ecommerce.payments.requested.v1");
        assertThat(record.key()).isEqualTo("101");
        assertThat(record.value()).isEqualTo(event);
        assertThat(headerValue(record, "event-id")).isEqualTo("event-1");
        assertThat(headerValue(record, "event-type")).isEqualTo(PaymentRequestedEvent.EVENT_TYPE);
    }

    private CompletableFuture<SendResult<String, PaymentRequestedEvent>> successfulFuture(PaymentRequestedEvent event) {
        ProducerRecord<String, PaymentRequestedEvent> record =
            new ProducerRecord<>("ecommerce.payments.requested.v1", event.orderId().toString(), event);

        RecordMetadata metadata = new RecordMetadata(
            new TopicPartition("ecommerce.payments.requested.v1", 0),
            0L,
            15,
            0L,
            0,
            0
        );

        return CompletableFuture.completedFuture(new SendResult<>(record, metadata));
    }

    private String headerValue(ProducerRecord<String, PaymentRequestedEvent> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}
