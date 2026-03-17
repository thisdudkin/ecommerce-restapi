package org.example.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record RefreshTokenRequest(

    @NotBlank
    String refreshToken

) implements Serializable { }
