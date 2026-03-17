package org.example.ecommerce.orders.dto.response;

import org.example.ecommerce.orders.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    UserResponse user,
    OrderStatus status,
    BigDecimal totalPrice,
    boolean deleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemResponse> orderItems
) { }
