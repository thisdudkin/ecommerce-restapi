package org.example.ecommerce.payments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payments.messaging")
public record PaymentMessagingProperties(
    String requestTopicName,
    String recordedTopicName,
    int topicPartitions,
    int topicReplicas,
    int topicMinInSyncReplicas,
    int outboxBatchSize
) { }
