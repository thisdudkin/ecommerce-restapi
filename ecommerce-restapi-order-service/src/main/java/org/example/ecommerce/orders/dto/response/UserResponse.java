package org.example.ecommerce.orders.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(
    Long id,
    String name,
    String surname,
    LocalDate birthDate,
    String email,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }
