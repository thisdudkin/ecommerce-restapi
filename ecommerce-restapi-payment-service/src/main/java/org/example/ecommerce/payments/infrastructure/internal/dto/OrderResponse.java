package org.example.ecommerce.payments.infrastructure.internal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    UserResponse user,
    OrderStatus status,
    BigDecimal totalPrice,
    boolean deleted,
    List<OrderItemResponse> orderItems,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }

record OrderItemResponse(
    Long id,
    Long itemId,
    String itemName,
    BigDecimal itemPrice,
    int quantity,
    BigDecimal subtotal,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }

