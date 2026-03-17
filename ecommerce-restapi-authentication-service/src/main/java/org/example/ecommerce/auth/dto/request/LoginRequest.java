package org.example.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record LoginRequest(

    @NotBlank
    @Size(max = 100)
    String login,

    @NotBlank
    @Size(min = 8, max = 100)
    String password

) implements Serializable { }
