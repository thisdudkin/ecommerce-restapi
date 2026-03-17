package org.example.ecommerce.auth.exception.custom;

public class CredentialAlreadyExistsException extends RuntimeException {
    public CredentialAlreadyExistsException(String message) {
        super(message);
    }
}
