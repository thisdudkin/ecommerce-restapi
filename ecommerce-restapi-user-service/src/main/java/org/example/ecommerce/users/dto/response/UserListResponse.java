package org.example.ecommerce.users.dto.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserListResponse(
    Long id,
    String name,
    String surname,
    LocalDate birthDate,
    String email,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable { }
