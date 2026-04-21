package org.example.ecommerce.gateway.domain.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class TokenValidationUnavailableException extends AuthenticationException {
    public TokenValidationUnavailableException(String message) {
        super(message);
    }

    public TokenValidationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
