package org.example.ecommerce.payments.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentV1Request(
    @NotNull(message = "Order id must not be null.")
    Long orderId,

    @NotNull(message = "Amount must not be null.")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0.")
    BigDecimal amount
) { }
