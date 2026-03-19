package org.example.ecommerce.users.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

public record PaymentCardRequest(
    @NotBlank(message = "Card number mustn't be blank.")
    @Size(max = 19, message = "Card number must be at most 19 characters.")
    String number,

    @NotBlank(message = "Card holder mustn't be blank.")
    @Size(max = 200, message = "Card holder must be at most 200 characters.")
    String holder,

    @NotNull(message = "Expiration date mustn't be null.")
    @Future(message = "Expiration date must be in the future.")
    LocalDate expirationDate
) implements Serializable {
}
