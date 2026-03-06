package org.example.ecommerce.users.dto.request;

import org.example.ecommerce.users.repository.enums.SortDirection;

import java.io.Serializable;
import java.time.LocalDateTime;

public record UserCursorPayload(
    LocalDateTime createdAt,
    Long id,
    SortDirection direction
) implements Serializable { }
