package org.example.ecommerce.orders.security.principal;

public record UserSecurityPrincipal(
    Long userId,
    String subject,
    boolean internal
) { }
