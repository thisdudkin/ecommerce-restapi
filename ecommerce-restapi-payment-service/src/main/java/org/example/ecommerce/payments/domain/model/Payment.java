package org.example.ecommerce.payments.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Payment(
    String id,
    Long orderId,
    Long userId,
    PaymentStatus status,
    Instant timestamp,
    BigDecimal amount
) { }
