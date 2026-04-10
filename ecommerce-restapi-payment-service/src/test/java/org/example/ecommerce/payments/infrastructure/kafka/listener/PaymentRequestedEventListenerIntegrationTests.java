package org.example.ecommerce.payments.infrastructure.kafka.listener;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.ecommerce.payments.Application;
import org.example.ecommerce.payments.config.PaymentMessagingProperties;
import org.example.ecommerce.payments.infrastructure.kafka.event.PaymentRequestedEvent;
import org.example.ecommerce.payments.infrastructure.mongo.document.PaymentDocument;
import org.example.ecommerce.payments.infrastructure.mongo.repository.PaymentDocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@Testcontainers
@EnableWireMock(
    @ConfigureWireMock(
        name = "random-org",
        baseUrlProperties = "clients.random-org.base-url"
    )
)
class PaymentRequestedEventListenerIntegrationTests {

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
        registry.add("clients.random-org.api-key", () -> "test-api-key");
        registry.add("clients.order-service.base-url", () -> "http://localhost:1");
        registry.add("payments.messaging.outbox-poll-delay-ms", () -> "600000");
    }

    @InjectWireMock("random-org")
    private WireMockServer wireMockServer;

    @Autowired
    private PaymentMessagingProperties paymentMessagingProperties;

    @Autowired
    private PaymentDocumentRepository paymentDocumentRepository;

    private KafkaTemplate<String, PaymentRequestedEvent> kafkaTemplate;

    @BeforeEach
    void setUp() {
        paymentDocumentRepository.deleteAll();

        Map<String, Object> producerProps = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class
        );

        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
        paymentDocumentRepository.deleteAll();
        if (kafkaTemplate != null) {
            kafkaTemplate.destroy();
        }
    }

    @Test
    void paymentRequestedEventShouldBeConsumedAndPersistPaymentInMongo() throws Exception {
        wireMockServer.stubFor(
            post(urlEqualTo("/json-rpc/4/invoke"))
                .willReturn(
                    com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "result": {
                                "random": {
                                  "data": [2],
                                  "completionTime": "2026-04-20 10:00:00Z"
                                },
                                "bitsUsed": 8,
                                "bitsLeft": 249992,
                                "requestsLeft": 999,
                                "advisoryDelay": 0
                              },
                              "id": 1
                            }
                            """)
                )
        );

        PaymentRequestedEvent event = new PaymentRequestedEvent(
            UUID.randomUUID().toString(),
            PaymentRequestedEvent.EVENT_TYPE,
            Instant.parse("2026-04-20T12:00:00Z"),
            321L,
            654L,
            new BigDecimal("45.00")
        );

        kafkaTemplate.send(paymentMessagingProperties.requestTopicName(), event.orderId().toString(), event).get();

        PaymentDocument saved = awaitPersistedPayment();

        assertThat(saved.orderId()).isEqualTo(321L);
        assertThat(saved.userId()).isEqualTo(654L);
        assertThat(saved.amount()).isEqualByComparingTo("45.00");
        assertThat(saved.status().name()).isEqualTo("SUCCESS");
        assertThat(saved.eventId()).isNotBlank();
        assertThat(saved.publishedAt()).isNull();
    }

    private PaymentDocument awaitPersistedPayment() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < deadline) {
            var all = paymentDocumentRepository.findAll();
            if (!all.isEmpty()) {
                return all.getFirst();
            }
            Thread.sleep(250);
        }

        throw new AssertionError("Payment document was not persisted within timeout");
    }
}
