package org.example.ecommerce.gateway.exception;

import org.springframework.security.authentication.BadCredentialsException;

public class MissingBearerTokenException extends BadCredentialsException {
    public MissingBearerTokenException(String message) {
        super(message);
    }
}
