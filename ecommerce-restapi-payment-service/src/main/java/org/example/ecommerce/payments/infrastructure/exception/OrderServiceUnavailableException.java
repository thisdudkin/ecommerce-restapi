package org.example.ecommerce.payments.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class OrderServiceUnavailableException extends InfrastructureException {
    public OrderServiceUnavailableException(String message) {
        super(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Order service unavailable",
            message
        );
    }

    public OrderServiceUnavailableException(String message, Throwable cause) {
        super(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Order service unavailable",
            message,
            cause
        );
    }
}
