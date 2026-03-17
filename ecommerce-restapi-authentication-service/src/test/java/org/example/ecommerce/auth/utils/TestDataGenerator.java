package org.example.ecommerce.auth.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.example.ecommerce.auth.dto.request.LoginRequest;
import org.example.ecommerce.auth.dto.request.RefreshTokenRequest;
import org.example.ecommerce.auth.dto.request.RegisterRequest;
import org.example.ecommerce.auth.dto.request.UserRequest;
import org.example.ecommerce.auth.dto.request.ValidateTokenRequest;
import org.example.ecommerce.auth.dto.response.TokenResponse;
import org.example.ecommerce.auth.dto.response.UserResponse;
import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.principal.AuthUserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

public final class TestDataGenerator {

    private static final RandomStringUtils RSU = RandomStringUtils.insecure();

    private TestDataGenerator() {
    }

    public static Long id() {
        return ThreadLocalRandom.current().nextLong(1, 10_000);
    }

    public static String login() {
        return "login-" + RSU.nextAlphabetic(8).toLowerCase();
    }

    public static String password() {
        return "password123";
    }

    public static String email() {
        return RSU.nextAlphabetic(10).toLowerCase() + "@test.com";
    }

    public static String name() {
        return "Name-" + RSU.nextAlphabetic(8);
    }

    public static String surname() {
        return "Surname-" + RSU.nextAlphabetic(10);
    }

    public static LocalDate birthDate() {
        return LocalDate.now().minusYears(
            ThreadLocalRandom.current().nextInt(18, 60)
        );
    }

    public static LocalDateTime datetime() {
        return LocalDateTime.now().minusDays(
            ThreadLocalRandom.current().nextInt(1, 365)
        );
    }

    public static UserRequest userRequest() {
        return new UserRequest(
            name(),
            surname(),
            email(),
            birthDate()
        );
    }

    public static RegisterRequest registerRequest() {
        return new RegisterRequest(
            login(),
            password(),
            userRequest()
        );
    }

    public static LoginRequest loginRequest() {
        return new LoginRequest(
            login(),
            password()
        );
    }

    public static RefreshTokenRequest refreshTokenRequest(String token) {
        return new RefreshTokenRequest(token);
    }

    public static ValidateTokenRequest validateTokenRequest(String token) {
        return new ValidateTokenRequest(token);
    }

    public static UserResponse userResponse(Long userId) {
        return new UserResponse(
            userId,
            name(),
            surname(),
            birthDate(),
            email(),
            true,
            datetime(),
            datetime()
        );
    }

    public static TokenResponse tokenResponse() {
        return new TokenResponse(
            "access-token",
            900L,
            "refresh-token",
            2_592_000L
        );
    }

    public static UserCredential userCredential(Long userId,
                                                String login,
                                                boolean active,
                                                Role role) {
        UserCredential credential = UserCredential.builder()
            .userId(userId)
            .login(login)
            .passwordHash("encoded-password")
            .role(role)
            .active(active)
            .createdAt(datetime())
            .updatedAt(datetime())
            .build();

        credential.setId(id());
        return credential;
    }

    public static AuthUserDetails authUserDetails(Long userId,
                                                  String login,
                                                  boolean active,
                                                  Role role) {
        return new AuthUserDetails(
            userCredential(userId, login, active, role)
        );
    }
}
