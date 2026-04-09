package org.example.ecommerce.orders.exception.custom.order;

public class OrderStatusInvalidException extends RuntimeException {
    public OrderStatusInvalidException(String message) {
        super(message);
    }
}
