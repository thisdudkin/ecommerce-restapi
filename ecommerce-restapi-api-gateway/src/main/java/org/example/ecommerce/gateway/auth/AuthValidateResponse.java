package org.example.ecommerce.gateway.auth;

public record AuthValidateResponse(
    boolean valid,
    Long userId,
    String role,
    String tokenType
) { }
