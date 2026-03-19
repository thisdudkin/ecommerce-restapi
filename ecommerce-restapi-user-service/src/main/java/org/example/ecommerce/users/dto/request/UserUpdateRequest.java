package org.example.ecommerce.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

public record UserUpdateRequest(
    @NotBlank(message = "Name mustn't be blank.")
    @Size(max = 100, message = "Name must be at most 100 characters.")
    String name,

    @NotBlank(message = "Surname mustn't be blank.")
    @Size(max = 100, message = "Surname must be at most 100 characters.")
    String surname,

    @NotNull(message = "Birthdate mustn't be null.")
    @Past(message = "Birthdate must be in the past.")
    LocalDate birthDate,

    @NotBlank(message = "Email mustn't be blank.")
    @Email(message = "Email must be valid.")
    @Size(max = 255, message = "Email must be at most 255 characters.")
    String email
) implements Serializable {
}
