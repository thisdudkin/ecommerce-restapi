package org.example.ecommerce.orders.dto.request;

import java.math.BigDecimal;

public record PaymentCreateRequest(
    Long orderId,
    BigDecimal amount
) { }
