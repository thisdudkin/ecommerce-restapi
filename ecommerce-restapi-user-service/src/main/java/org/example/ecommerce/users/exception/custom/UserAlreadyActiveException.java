package org.example.ecommerce.users.exception.custom;

public class UserAlreadyActiveException extends RuntimeException {
    public UserAlreadyActiveException(Long id) {
        super("User with id " + id + " is already active.");
    }
}
