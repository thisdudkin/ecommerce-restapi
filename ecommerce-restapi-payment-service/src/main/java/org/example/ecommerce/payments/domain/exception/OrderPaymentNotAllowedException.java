package org.example.ecommerce.payments.domain.exception;

import lombok.Getter;
import org.example.ecommerce.payments.infrastructure.internal.dto.OrderStatus;
import org.springframework.http.HttpStatus;

@Getter
public class OrderPaymentNotAllowedException extends DomainException {

    private final Long orderId;
    private final OrderStatus orderStatus;

    public OrderPaymentNotAllowedException(Long orderId, OrderStatus orderStatus) {
        super(
            HttpStatus.CONFLICT,
            "Order payment not allowed",
            "Order with ID: %s cannot be paid in status %s"
                .formatted(orderId, orderStatus)
        );
        this.orderId = orderId;
        this.orderStatus = orderStatus;
    }

}
