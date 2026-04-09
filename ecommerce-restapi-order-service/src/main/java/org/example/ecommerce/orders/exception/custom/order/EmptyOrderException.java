package org.example.ecommerce.orders.exception.custom.order;

public class EmptyOrderException extends RuntimeException {
    public EmptyOrderException() {
        super("Empty order can't be paid");
    }
}
