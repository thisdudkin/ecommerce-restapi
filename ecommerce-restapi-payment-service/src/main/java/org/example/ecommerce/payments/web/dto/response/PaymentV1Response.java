package org.example.ecommerce.payments.web.dto.response;

import org.example.ecommerce.payments.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentV1Response(
    String id,
    Long orderId,
    Long userId,
    PaymentStatus status,
    Instant timestamp,
    BigDecimal amount
) { }
