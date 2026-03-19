package org.example.ecommerce.users.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

@EnableCaching
@Configuration
public class RedisCacheConfig {

    public static final String USER_WITH_CARDS = "users-with-cards";

    @Bean
    public CacheManager cacheManager(
        RedisConnectionFactory connectionFactory,
        @Value("${spring.cache.redis.time-to-live}") Duration ttl
    ) {
        GenericJacksonJsonRedisSerializer serializer =
            GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(
                    BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("org.example.ecommerce.users.dto.response")
                        .allowIfSubType("java.util")
                        .build()
                )
                .build();

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .transactionAware()
            .build();
    }

}
