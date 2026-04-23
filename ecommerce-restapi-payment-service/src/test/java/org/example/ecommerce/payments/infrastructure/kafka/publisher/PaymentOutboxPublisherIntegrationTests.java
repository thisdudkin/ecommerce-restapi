package org.example.ecommerce.payments.infrastructure.kafka.publisher;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.ecommerce.payments.Application;
import org.example.ecommerce.payments.config.PaymentMessagingProperties;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.infrastructure.kafka.event.PaymentRecordedEvent;
import org.example.ecommerce.payments.infrastructure.mongo.document.PaymentDocument;
import org.example.ecommerce.payments.infrastructure.mongo.repository.PaymentDocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = Application.class)
@Testcontainers
class PaymentOutboxPublisherIntegrationTests {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("apache/kafka:4.1.0")
    );

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("clients.random-org.base-url", () -> "http://localhost:1");
        registry.add("clients.random-org.api-key", () -> "test-api-key");
        registry.add("clients.order-service.base-url", () -> "http://localhost:1");
        registry.add("payments.messaging.outbox-poll-delay-ms", () -> "600000");
    }

    @Autowired
    private PaymentOutboxPublisher publisher;

    @Autowired
    private PaymentDocumentRepository paymentDocumentRepository;

    @Autowired
    private PaymentMessagingProperties paymentMessagingProperties;

    private Consumer<String, PaymentRecordedEvent> consumer;

    @BeforeEach
    void setUp() {
        paymentDocumentRepository.deleteAll();

        Map<String, Object> consumerProps = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG, "payment-outbox-it-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class,
            JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, PaymentRecordedEvent.class.getName(),
            JacksonJsonDeserializer.TRUSTED_PACKAGES, "org.example.ecommerce.payments.infrastructure.kafka.event",
            JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false
        );

        consumer = new DefaultKafkaConsumerFactory<String, PaymentRecordedEvent>(consumerProps).createConsumer();
        consumer.subscribe(List.of(paymentMessagingProperties.recordedTopicName()));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
        paymentDocumentRepository.deleteAll();
    }

    @Test
    void publishPendingEventsShouldPublishKafkaRecordAndMarkMongoDocumentAsPublished() {
        paymentDocumentRepository.save(new PaymentDocument(
            "payment-1",
            9001L,
            77L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T12:00:00Z"),
            new BigDecimal("88.40"),
            "event-9001",
            null
        ));

        publisher.publishPendingEvents();

        ConsumerRecord<String, PaymentRecordedEvent> kafkaRecord = awaitSingleRecord();

        assertThat(kafkaRecord.topic()).isEqualTo(paymentMessagingProperties.recordedTopicName());
        assertThat(kafkaRecord.key()).isEqualTo("9001");
        assertThat(kafkaRecord.value().eventId()).isEqualTo("event-9001");
        assertThat(kafkaRecord.value().eventType()).isEqualTo(PaymentRecordedEvent.EVENT_TYPE);
        assertThat(kafkaRecord.value().paymentId()).isEqualTo("payment-1");
        assertThat(kafkaRecord.value().orderId()).isEqualTo(9001L);
        assertThat(kafkaRecord.value().userId()).isEqualTo(77L);
        assertThat(kafkaRecord.value().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(kafkaRecord.value().amount()).isEqualByComparingTo("88.40");

        Header eventIdHeader = kafkaRecord.headers().lastHeader("event-id");
        Header eventTypeHeader = kafkaRecord.headers().lastHeader("event-type");

        assertThat(new String(eventIdHeader.value(), StandardCharsets.UTF_8)).isEqualTo("event-9001");
        assertThat(new String(eventTypeHeader.value(), StandardCharsets.UTF_8)).isEqualTo(PaymentRecordedEvent.EVENT_TYPE);

        PaymentDocument updated = paymentDocumentRepository.findById("payment-1").orElseThrow();
        assertThat(updated.publishedAt()).isNotNull();
    }

    private ConsumerRecord<String, PaymentRecordedEvent> awaitSingleRecord() {
        long deadline = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, PaymentRecordedEvent> records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }

        fail("Kafka record was not received within timeout");
        return null;
    }

}
