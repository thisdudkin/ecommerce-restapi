package org.example.ecommerce.orders.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.example.ecommerce.orders.enums.OrderStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

public record OrderScrollRequest(
    @Max(50)
    @Positive
    Integer size,
    OrderStatus status,
    LocalDateTime from,
    LocalDateTime to,
    String token
) implements Serializable { }
