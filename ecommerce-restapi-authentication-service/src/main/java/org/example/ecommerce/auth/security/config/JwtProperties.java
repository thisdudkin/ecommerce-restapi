package org.example.ecommerce.auth.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
    String publicKey,
    String privateKey,
    long accessExpiration,
    long refreshExpiration,
    String issuer
) { }
