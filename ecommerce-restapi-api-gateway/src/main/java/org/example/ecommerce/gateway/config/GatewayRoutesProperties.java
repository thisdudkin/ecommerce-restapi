package org.example.ecommerce.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.routes")
public record GatewayRoutesProperties(
    String authServiceUri,
    String userServiceUri,
    String orderServiceUri
) { }
