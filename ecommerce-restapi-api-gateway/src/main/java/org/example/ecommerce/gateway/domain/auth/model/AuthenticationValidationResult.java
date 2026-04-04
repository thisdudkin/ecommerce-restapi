package org.example.ecommerce.gateway.domain.auth.model;

import org.example.ecommerce.gateway.domain.auth.exception.InvalidTokenException;
import org.springframework.util.StringUtils;

public record AuthenticationValidationResult(
    Long userId,
    String role,
    String tokenType
) {

    public AuthenticationValidationResult {
        if (userId == null) {
            throw new InvalidTokenException("Authenticated user ID is missing");
        }
        if (!StringUtils.hasText(role)) {
            throw new InvalidTokenException("Authenticated user role is missing");
        }
        if (!StringUtils.hasText(tokenType)) {
            throw new InvalidTokenException("Authenticated token type is missing");
        }
    }

}
