package org.example.ecommerce.users.exception.custom;

public class UserEmailAlreadyExistsException extends RuntimeException {
    public UserEmailAlreadyExistsException(String email) {
        super("User with email already exists: " + email);
    }
}
