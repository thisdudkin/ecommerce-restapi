package org.example.ecommerce.auth.exception.custom;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
