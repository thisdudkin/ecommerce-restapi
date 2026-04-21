package org.example.ecommerce.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.auth-client")
public record AuthenticationClientProperties(
    String baseUrl,
    String validatePath,
    int connectTimeoutMs,
    int readTimeoutMs
) { }
