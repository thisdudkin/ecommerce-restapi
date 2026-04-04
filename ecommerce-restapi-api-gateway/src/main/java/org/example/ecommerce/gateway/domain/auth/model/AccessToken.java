package org.example.ecommerce.gateway.domain.auth.model;

import org.example.ecommerce.gateway.domain.auth.exception.MissingBearerTokenException;
import org.springframework.util.StringUtils;

public record AccessToken(
    String value
) {

    public AccessToken {
        if (!StringUtils.hasText(value)) {
            throw new MissingBearerTokenException("Bearer token is empty");
        }
    }

    public static AccessToken fromAuthorizationHeader(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new MissingBearerTokenException("Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7).trim();
        return new AccessToken(token);
    }

}
