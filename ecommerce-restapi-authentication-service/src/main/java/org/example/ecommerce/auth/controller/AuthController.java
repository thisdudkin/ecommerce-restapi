package org.example.ecommerce.auth.controller;

import jakarta.validation.Valid;
import org.example.ecommerce.auth.dto.request.LoginRequest;
import org.example.ecommerce.auth.dto.request.RefreshTokenRequest;
import org.example.ecommerce.auth.dto.request.RegisterRequest;
import org.example.ecommerce.auth.dto.request.ValidateTokenRequest;
import org.example.ecommerce.auth.dto.response.TokenResponse;
import org.example.ecommerce.auth.dto.response.ValidateTokenResponse;
import org.example.ecommerce.auth.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user credential and returns an access/refresh token pair.
     *
     * @param request registration payload containing login, password and user profile data
     * @return created token pair for the newly registered user
     */
    @PostMapping("/credentials")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    /**
     * Authenticates a user by login and password and returns a new access/refresh token pair.
     *
     * @param request login payload containing user credentials
     * @return issued token pair for the authenticated user
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Rotates a refresh token and returns a new access/refresh token pair.
     *
     * @param request payload containing the refresh token to be exchanged
     * @return newly issued token pair if the refresh token is valid and active
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Revokes the provided refresh token so it can no longer be used.
     *
     * @param request payload containing the refresh token to revoke
     * @return empty response when the token is revoked successfully
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Validates a JWT and returns its verification result and resolved claims.
     *
     * @param request payload containing the token to validate
     * @return validation result with resolved user and token metadata when valid
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validate(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(authService.validate(request));
    }

}
