package org.example.ecommerce.gateway.security;

public record GatewayPrincipal(
    Long userId,
    String role,
    String tokenType
) { }
