package org.example.ecommerce.gateway.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class TokenValidationServiceUnavailableException extends AuthenticationServiceException {
    public TokenValidationServiceUnavailableException(String message) {
        super(message);
    }
}
