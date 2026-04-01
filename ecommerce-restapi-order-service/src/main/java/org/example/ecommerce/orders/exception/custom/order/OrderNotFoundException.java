package org.example.ecommerce.orders.exception.custom.order;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long itemId) {
        super("Order not found for ID: '%s'".formatted(itemId));
    }
}
