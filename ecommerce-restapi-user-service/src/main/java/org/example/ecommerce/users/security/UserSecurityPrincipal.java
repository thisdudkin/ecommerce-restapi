package org.example.ecommerce.users.security;

public record UserSecurityPrincipal(
    Long userId,
    String role
) { }
