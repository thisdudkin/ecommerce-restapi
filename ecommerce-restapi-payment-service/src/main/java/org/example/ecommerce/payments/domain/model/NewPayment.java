package org.example.ecommerce.payments.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder(toBuilder = true)
public record NewPayment(
    Long orderId,
    Long userId,
    PaymentStatus status,
    Instant timestamp,
    BigDecimal amount
) { }
