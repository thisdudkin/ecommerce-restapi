package org.example.ecommerce.orders.dto.response;

import org.example.ecommerce.orders.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
    String id,
    Long orderId,
    Long userId,
    PaymentStatus status,
    Instant timestamp,
    BigDecimal amount
) { }
