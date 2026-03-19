package org.example.ecommerce.users.dto.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long id,
    String name,
    String surname,
    LocalDate birthDate,
    String email,
    Boolean active,
    Set<PaymentCardResponse> paymentCards,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {
}
