package org.example.ecommerce.payments.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OrderAlreadyPaidException extends DomainException {

    private final Long orderId;

    public OrderAlreadyPaidException(Long orderId) {
        super(
            HttpStatus.CONFLICT,
            "Order already paid",
            "Order with ID: %s is already paid.".formatted(orderId)
        );
        this.orderId = orderId;
    }

}
