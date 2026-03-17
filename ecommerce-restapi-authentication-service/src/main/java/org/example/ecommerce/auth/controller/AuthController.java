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

    @PostMapping("/credentials")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validate(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(authService.validate(request));
    }

}
