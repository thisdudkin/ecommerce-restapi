package org.example.ecommerce.orders.exception.custom.feign;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
