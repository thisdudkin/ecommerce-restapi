package org.example.ecommerce.orders.exception.custom.item;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(Long itemId) {
        super("Item not found for ID: '%s'".formatted(itemId));
    }
}
