package org.example.ecommerce.orders.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

public record OrderChangeQuantityRequest(
    @NotNull
    Long itemId,
    @Positive
    int quantity
) implements Serializable { }
