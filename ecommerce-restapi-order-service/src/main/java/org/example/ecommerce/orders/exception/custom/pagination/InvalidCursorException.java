package org.example.ecommerce.orders.exception.custom.pagination;

public class InvalidCursorException extends RuntimeException {
    public InvalidCursorException(String message, Throwable cause) {
        super(message, cause);
    }
}
