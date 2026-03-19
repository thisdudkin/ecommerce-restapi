package org.example.ecommerce.users.exception.custom;

public class UserAlreadyInactiveException extends RuntimeException {
    public UserAlreadyInactiveException(Long id) {
        super("User with id " + id + " is already inactive.");
    }
}
