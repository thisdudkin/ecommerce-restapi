package org.example.ecommerce.auth.exception.custom;

public class InactiveUserCredentialException extends RuntimeException {
    public InactiveUserCredentialException(String message) {
        super(message);
    }
}
