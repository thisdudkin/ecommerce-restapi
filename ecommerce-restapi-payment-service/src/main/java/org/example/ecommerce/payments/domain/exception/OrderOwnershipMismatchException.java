package org.example.ecommerce.payments.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OrderOwnershipMismatchException extends DomainException {

    private final Long orderId;
    private final Long expectedUserId;
    private final Long actualUserId;

    public OrderOwnershipMismatchException(Long orderId, Long expectedUserId, Long actualUserId) {
        super(
            HttpStatus.CONFLICT,
            "Order ownership mismatch",
            "Order with ID: %s doesn't belong to user with ID: %s"
                .formatted(orderId, expectedUserId)
        );
        this.orderId = orderId;
        this.expectedUserId = expectedUserId;
        this.actualUserId = actualUserId;
    }

}
