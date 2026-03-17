package org.example.ecommerce.users.security.jwt;

import org.example.ecommerce.users.security.enums.JwtType;

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
