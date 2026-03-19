package org.example.ecommerce.users.dto.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentCardResponse(
    Long id,
    String number,
    String holder,
    LocalDate expirationDate,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {
}
