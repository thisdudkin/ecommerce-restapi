package org.example.ecommerce.gateway.domain.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class MissingBearerTokenException extends AuthenticationException {
    public MissingBearerTokenException(String message) {
        super(message);
    }
}
