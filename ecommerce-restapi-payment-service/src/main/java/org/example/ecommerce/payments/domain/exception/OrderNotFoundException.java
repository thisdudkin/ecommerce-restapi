package org.example.ecommerce.payments.domain.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException() {
        super(
            HttpStatus.NOT_FOUND,
            "Order not found",
            "Order was not found."
        );
    }
}
