package org.example.ecommerce.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.routes")
public record GatewayRoutesProperties(
    String userServiceUri,
    String authServiceUri,
    String orderServiceUri
) { }
