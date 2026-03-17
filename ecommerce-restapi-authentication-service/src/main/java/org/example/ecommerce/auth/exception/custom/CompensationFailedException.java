package org.example.ecommerce.auth.exception.custom;

public class CompensationFailedException extends RuntimeException {
    public CompensationFailedException(String message, Throwable cause) {
        super(message);
    }
}
