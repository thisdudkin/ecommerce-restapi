package org.example.ecommerce.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.auth-client")
public record AuthClientProperties(
    String baseUrl,
    String validatePath,
    int connectTimeoutMs,
    int readTimeoutMs
) { }
