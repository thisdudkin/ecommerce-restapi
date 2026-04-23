package org.example.ecommerce.payments.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class RandomNumberGenerationException extends InfrastructureException {
    public RandomNumberGenerationException(String message) {
        super(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Random number generation failed",
            message
        );
    }

    public RandomNumberGenerationException(String message, Throwable cause) {
        super(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Random number generation failed",
            message,
            cause
        );
    }
}
