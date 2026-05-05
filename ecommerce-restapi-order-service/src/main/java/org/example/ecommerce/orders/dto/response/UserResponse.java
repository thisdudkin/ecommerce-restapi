package org.example.ecommerce.orders.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
    Long id,
    String name,
    String surname,
    LocalDate birthDate,
    String email,
    boolean active,
    List<PaymentCardResponse> paymentCards,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
