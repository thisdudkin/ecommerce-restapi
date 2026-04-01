package org.example.ecommerce.orders.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemResponse(
    Long id,
    Long itemId,
    String itemName,
    BigDecimal itemPrice,
    int quantity,
    BigDecimal subtotal,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }
