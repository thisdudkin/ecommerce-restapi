package org.example.ecommerce.gateway.infrastructure.auth.client.dto;

public record AuthenticationValidateResponse(
    boolean valid,
    Long userId,
    String role,
    String tokenType
) { }
