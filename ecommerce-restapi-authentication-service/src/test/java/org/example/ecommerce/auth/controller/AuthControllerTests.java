package org.example.ecommerce.auth.controller;

import org.example.ecommerce.auth.dto.request.LoginRequest;
import org.example.ecommerce.auth.dto.request.RefreshTokenRequest;
import org.example.ecommerce.auth.dto.request.RegisterRequest;
import org.example.ecommerce.auth.dto.request.UserRequest;
import org.example.ecommerce.auth.exception.custom.*;
import org.example.ecommerce.auth.security.config.SecurityConfig;
import org.example.ecommerce.auth.security.exception.InvalidJwtException;
import org.example.ecommerce.auth.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = AuthController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerShouldReturnConflictWhenCredentialAlreadyExists() throws Exception {
        RegisterRequest request = validRegisterRequest();

        when(authService.register(request))
            .thenThrow(new CredentialAlreadyExistsException("Credential already exists"));

        mockMvc.perform(post("/api/v1/auth/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Credential already exists"));
    }

    @Test
    void registerShouldReturnConflictWhenUserAlreadyExists() throws Exception {
        RegisterRequest request = validRegisterRequest();

        when(authService.register(request))
            .thenThrow(new UserAlreadyExistsException("User already exists", "User email already exists"));

        mockMvc.perform(post("/api/v1/auth/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("User already exists"))
            .andExpect(jsonPath("$.detail").value("User email already exists"));
    }

    @Test
    void registerShouldReturnServiceUnavailableWhenUserServiceIsDown() throws Exception {
        RegisterRequest request = validRegisterRequest();

        when(authService.register(request))
            .thenThrow(new DownstreamServiceUnavailableException("User service is temporarily unavailable"));

        mockMvc.perform(post("/api/v1/auth/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.title").value("Service unavailable"));
    }

    @Test
    void registerShouldReturnInternalServerErrorWhenCompensationFails() throws Exception {
        RegisterRequest request = validRegisterRequest();

        when(authService.register(request))
            .thenThrow(new CompensationFailedException("Compensation failed", new RuntimeException("boom")));

        mockMvc.perform(post("/api/v1/auth/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.title").value("Registration partially failed"));
    }

    @Test
    void registerShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "",
            "",
            new UserRequest("", "", "bad-email", LocalDate.now().plusDays(1))
        );

        mockMvc.perform(post("/api/v1/auth/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest("alex.user", "wrong-password");

        when(authService.login(request))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Authentication failed"))
            .andExpect(jsonPath("$.detail").value("Invalid login or password"));
    }

    @Test
    void loginShouldReturnForbiddenWhenCredentialIsInactive() throws Exception {
        LoginRequest request = new LoginRequest("inactive.user", "password123");

        when(authService.login(request))
            .thenThrow(new InactiveUserCredentialException("Credential inactive"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Credential inactive"));
    }

    @Test
    void loginShouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs() throws Exception {
        LoginRequest request = new LoginRequest("alex.user", "password123");

        when(authService.login(request))
            .thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.title").value("Internal server error"));
    }

    @Test
    void refreshShouldReturnNotFoundWhenCredentialDoesNotExist() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        when(authService.refresh(request))
            .thenThrow(new CredentialNotFoundException("Credential not found"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Credential not found"));
    }

    @Test
    void refreshShouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("broken-token");

        when(authService.refresh(request))
            .thenThrow(new InvalidJwtException("JWT token is invalid"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Invalid token"));
    }

    @Test
    void refreshShouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Malformed request body"));
    }

    private RegisterRequest validRegisterRequest() {
        return new RegisterRequest(
            "alex.user",
            "password123",
            new UserRequest(
                "Alex",
                "Dudkin",
                "alex.user@test.com",
                LocalDate.of(2000, 1, 1)
            )
        );
    }

}
