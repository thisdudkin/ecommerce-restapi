package org.example.ecommerce.users.security.principal;

public record UserSecurityPrincipal(
    Long userId,
    String subject,
    boolean internal
) { }
