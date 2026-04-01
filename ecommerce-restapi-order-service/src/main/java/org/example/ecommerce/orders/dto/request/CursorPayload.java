package org.example.ecommerce.orders.dto.request;

import java.time.LocalDateTime;

public record CursorPayload(
    LocalDateTime createdAt,
    Long id
) { }
