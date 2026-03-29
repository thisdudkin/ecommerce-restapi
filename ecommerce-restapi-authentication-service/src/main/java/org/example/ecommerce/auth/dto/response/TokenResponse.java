package org.example.ecommerce.auth.dto.response;

import java.io.Serializable;

public record TokenResponse(
    String accessToken,
    long expiresIn,
    String refreshToken,
    long refreshExpiresIn
) implements Serializable { }
