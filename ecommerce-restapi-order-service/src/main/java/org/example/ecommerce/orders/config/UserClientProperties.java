package org.example.ecommerce.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "clients.user-service")
public record UserClientProperties(
    String baseUrl,
    Duration connectTimeout,
    Duration readTimeout
) { }
