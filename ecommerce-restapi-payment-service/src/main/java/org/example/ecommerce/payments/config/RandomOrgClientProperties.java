package org.example.ecommerce.payments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients.random-org")
public record RandomOrgClientProperties(
    String baseUrl,
    String apiKey
) { }
