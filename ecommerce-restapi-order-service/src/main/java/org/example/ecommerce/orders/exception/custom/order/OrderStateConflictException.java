package org.example.ecommerce.orders.exception.custom.order;

public class OrderStateConflictException extends RuntimeException {
    public OrderStateConflictException(String message) {
        super(message);
    }
}
