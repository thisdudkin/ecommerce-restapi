package org.example.ecommerce.payments.infrastructure.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class InfrastructureException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    protected InfrastructureException(HttpStatus status, String title, String message) {
        super(message);
        this.status = status;
        this.title = title;
    }

    protected InfrastructureException(HttpStatus status, String title, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.title = title;
    }

}
