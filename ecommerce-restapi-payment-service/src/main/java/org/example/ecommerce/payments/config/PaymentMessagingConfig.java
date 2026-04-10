package org.example.ecommerce.payments.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class PaymentMessagingConfig {

    private final PaymentMessagingProperties properties;

    @Bean
    public NewTopic paymentRecordedTopic() {
        return TopicBuilder.name(properties.recordedTopicName())
            .partitions(properties.topicPartitions())
            .replicas(properties.topicReplicas())
            .config(
                TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                Integer.toString(properties.topicMinInSyncReplicas())
            )
            .config(TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG, Boolean.FALSE.toString())
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build();
    }

    @Bean
    public NewTopic paymentRequestTopic() {
        return TopicBuilder.name(properties.requestTopicName())
            .partitions(properties.topicPartitions())
            .replicas(properties.topicReplicas())
            .config(
                TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                Integer.toString(properties.topicMinInSyncReplicas())
            )
            .config(TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG, Boolean.FALSE.toString())
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "producer")
            .build();
    }

}
