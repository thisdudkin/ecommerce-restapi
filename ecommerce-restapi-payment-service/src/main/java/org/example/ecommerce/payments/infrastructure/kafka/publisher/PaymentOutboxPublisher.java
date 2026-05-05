package org.example.ecommerce.payments.infrastructure.kafka.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.ecommerce.payments.config.PaymentMessagingProperties;
import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.domain.repository.PaymentRepository;
import org.example.ecommerce.payments.infrastructure.kafka.event.PaymentRecordedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxPublisher {

    private final PaymentRepository repository;
    private final KafkaTemplate<String, PaymentRecordedEvent> kafkaTemplate;
    private final PaymentMessagingProperties properties;

    @Scheduled(fixedDelayString = "${payments.messaging.outbox-poll-delay-ms:250}")
    public void publishPendingEvents() {
        List<PaymentOutboxRecord> pending = repository.findPendingPublicationBatch(properties.outboxBatchSize());

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Found {} payment events pending publication", pending.size());

        List<PublishAttempt> attempts = pending.stream()
            .map(record -> new PublishAttempt(record, send(record)))
            .toList();

        List<String> publishedPaymentIds = new ArrayList<>(attempts.size());

        for (PublishAttempt attempt : attempts) {
            try {
                SendResult<String, PaymentRecordedEvent> result = attempt.future().join();
                publishedPaymentIds.add(attempt.record.paymentId());

                log.debug(
                    "Payment event published: Payment ID: {}, Topic: {}, Partition: {}, Offset: {}",
                    attempt.record.paymentId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
                );
            } catch (RuntimeException e) {
                log.error(
                    "Failed to publish payment event: paymentId={}, orderId={}, eventId={}",
                    attempt.record().paymentId(),
                    attempt.record().orderId(),
                    attempt.record().eventId(),
                    e
                );
            }
        }

        if (publishedPaymentIds.isEmpty()) {
            return;
        }

        repository.markPublished(publishedPaymentIds, Instant.now());

        log.info("Marked {} payment events as published", publishedPaymentIds.size());
    }

    private CompletableFuture<SendResult<String, PaymentRecordedEvent>> send(PaymentOutboxRecord outboxRecord) {
        PaymentRecordedEvent event = PaymentRecordedEvent.from(outboxRecord);

        ProducerRecord<String, PaymentRecordedEvent> record = new ProducerRecord<>(
            properties.recordedTopicName(),
            outboxRecord.orderId().toString(),
            event
        );

        record.headers().add("event-id", outboxRecord.eventId().getBytes(StandardCharsets.UTF_8));
        record.headers().add("event-type", PaymentRecordedEvent.EVENT_TYPE.getBytes(StandardCharsets.UTF_8));

        return kafkaTemplate.send(record);
    }


    private record PublishAttempt(
        PaymentOutboxRecord record,
        CompletableFuture<SendResult<String, PaymentRecordedEvent>> future
    ) { }

}
