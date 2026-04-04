package org.example.ecommerce.orders.security;

public record UserSecurityPrincipal(
    Long userId,
    String subject,
    boolean internal
) { }
