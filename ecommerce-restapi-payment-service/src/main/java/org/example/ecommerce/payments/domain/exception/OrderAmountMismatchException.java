package org.example.ecommerce.payments.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

@Getter
public class OrderAmountMismatchException extends DomainException {

    private final Long orderId;
    private final BigDecimal expectedAmount;
    private final BigDecimal actualAmount;

    public OrderAmountMismatchException(Long orderId, BigDecimal expectedAmount, BigDecimal actualAmount) {
        super(
            HttpStatus.CONFLICT,
            "Order amount mismatch",
            "Order amount mismatch for order with ID: %s. Expected: %s, Actual: %s."
                .formatted(orderId, expectedAmount, actualAmount)
        );
        this.orderId = orderId;
        this.expectedAmount = expectedAmount;
        this.actualAmount = actualAmount;
    }

}
