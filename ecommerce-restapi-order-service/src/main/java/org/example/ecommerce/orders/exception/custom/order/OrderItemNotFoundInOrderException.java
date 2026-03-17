package org.example.ecommerce.orders.exception.custom.order;

public class OrderItemNotFoundInOrderException extends RuntimeException {
    public OrderItemNotFoundInOrderException(Long itemId) {
        super("Item %d not found in order".formatted(itemId));
    }
}
