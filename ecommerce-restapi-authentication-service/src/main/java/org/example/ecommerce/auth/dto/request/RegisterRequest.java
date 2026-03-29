package org.example.ecommerce.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record RegisterRequest(

    @NotBlank
    @Size(min = 4, max = 100)
    String login,

    @NotBlank
    @Size(min = 8, max = 100)
    String password,

    @Valid
    @NotNull
    UserRequest user

) implements Serializable { }
