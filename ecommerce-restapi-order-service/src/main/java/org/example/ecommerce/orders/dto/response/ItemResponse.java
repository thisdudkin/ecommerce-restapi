package org.example.ecommerce.orders.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemResponse(
    Long id,
    String name,
    BigDecimal price,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }
