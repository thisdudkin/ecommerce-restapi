package org.example.ecommerce.orders.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

public record ItemScrollRequest(
    @Max(50)
    @Positive
    Integer size,
    String token
) implements Serializable { }
