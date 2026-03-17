package org.example.ecommerce.orders.security.jwt;

import org.example.ecommerce.orders.security.enums.JwtType;

import java.time.Instant;

public record VerifiedJwtClaims(
    Long userId,
    String subject,
    String role,
    JwtType tokenType,
    Boolean internal,
    String serviceName,
    Instant issuedAt,
    Instant expiresAt
) { }
