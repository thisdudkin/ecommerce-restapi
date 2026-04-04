package org.example.ecommerce.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.http-timeouts")
public record HttpTimeoutProperties(
    int connectTimeoutMs,
    int responseTimeoutMs
) { }
