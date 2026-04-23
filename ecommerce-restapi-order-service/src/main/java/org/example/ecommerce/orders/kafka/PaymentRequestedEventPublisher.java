package org.example.ecommerce.orders.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PaymentRequestedEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(PaymentRequestedEventPublisher.class);

    private final KafkaTemplate<String, PaymentRequestedEvent> kafkaTemplate;
    private final String topicName;

    public PaymentRequestedEventPublisher(KafkaTemplate<String, PaymentRequestedEvent> kafkaTemplate,
                                          @Value("${orders.payment-requests.topic-name}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publish(PaymentRequestedEvent event) {
        var record = new ProducerRecord<>(
            topicName,
            event.orderId().toString(),
            event
        );

        record.headers().add("event-id", event.eventId().getBytes(StandardCharsets.UTF_8));
        record.headers().add("event-type", event.eventType().getBytes(StandardCharsets.UTF_8));

        SendResult<String, PaymentRequestedEvent> result = kafkaTemplate.send(record).join();

        log.info(
            "Published payment request event. Event ID: {}, Order ID: {}, Topic: {}, Partition: {}, Offset: {}",
            event.eventId(),
            event.orderId(),
            result.getRecordMetadata().topic(),
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset()
        );
    }

}
