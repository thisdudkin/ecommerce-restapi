package org.example.ecommerce.gateway.web.security;

public record GatewayPrincipal(
    Long userId,
    String role,
    String tokenType
) { }
