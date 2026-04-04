package org.example.ecommerce.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gateway.auth-cache")
public record AuthenticationCacheProperties(
    Duration ttl,
    String keyPrefix
) { }
