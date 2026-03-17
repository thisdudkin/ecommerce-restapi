package org.example.ecommerce.auth.dto.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String name,
    String surname,
    LocalDate birthDate,
    String email,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {
}
