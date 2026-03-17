package org.example.ecommerce.users.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
    String publicKey,
    String issuer,
    String trustedInternalService
) { }
