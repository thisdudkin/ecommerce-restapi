package org.example.ecommerce.auth.security.jwt;

import org.example.ecommerce.auth.security.enums.JwtType;
import org.example.ecommerce.auth.security.enums.Role;

import java.time.Instant;

public record JwtClaims(
    Long userId,
    String subject,
    Role role,
    JwtType tokenType,
    Boolean internal,
    String serviceName,
    Instant issuedAt,
    Instant expiresAt
) { }
