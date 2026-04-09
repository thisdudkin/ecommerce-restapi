package org.example.ecommerce.orders.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.math.BigDecimal;

public record ItemCreateRequest(
    @NotBlank
    String name,
    @NotNull
    @PositiveOrZero
    BigDecimal price
) implements Serializable { }
