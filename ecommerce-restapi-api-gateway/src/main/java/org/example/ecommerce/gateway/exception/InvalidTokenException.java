package org.example.ecommerce.gateway.exception;

import org.springframework.security.authentication.BadCredentialsException;

public class InvalidTokenException extends BadCredentialsException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
