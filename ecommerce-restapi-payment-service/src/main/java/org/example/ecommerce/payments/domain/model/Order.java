package org.example.ecommerce.payments.domain.model;

import org.example.ecommerce.payments.infrastructure.internal.dto.OrderStatus;

import java.math.BigDecimal;

public record Order(
    Long id,
    Long userId,
    OrderStatus status,
    BigDecimal totalPrice,
    boolean deleted
) { }
