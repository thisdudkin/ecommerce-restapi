package org.example.ecommerce.payments.infrastructure.security;

public record UserSecurityPrincipal(
    Long userId,
    String subject,
    boolean internal
) { }
