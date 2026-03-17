package org.example.ecommerce.auth.integration;

import org.example.ecommerce.auth.client.UserClient;
import org.example.ecommerce.auth.dto.request.*;
import org.example.ecommerce.auth.dto.response.TokenResponse;
import org.example.ecommerce.auth.dto.response.UserResponse;
import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.repository.UserCredentialRepository;
import org.example.ecommerce.auth.security.enums.JwtType;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.jwt.JwtClaims;
import org.example.ecommerce.auth.security.jwt.JwtService;
import org.example.ecommerce.auth.utils.TestJwtKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Sql("classpath:insert-auth.sql")
class AuthControllerIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.0");

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.public-key", TestJwtKeys::publicKeyBase64);
        registry.add("security.jwt.private-key", TestJwtKeys::privateKeyBase64);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserClient userClient;

    @Test
    void registerShouldCreateCredentialAndReturnTokens() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "new.user",
            "password123",
            new UserRequest(
                "Alex",
                "Dudkin",
                "new.user@test.com",
                LocalDate.of(2000, 1, 1)
            )
        );
        UserResponse createdUser = new UserResponse(
            500L,
            "Alex",
            "Dudkin",
            LocalDate.of(2000, 1, 1),
            "new.user@test.com",
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(userClient.create(request.user())).thenReturn(createdUser);

        String content = mockMvc.perform(post("/api/v1/auth/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        TokenResponse response = objectMapper.readValue(content, TokenResponse.class);
        JwtClaims accessClaims = jwtService.parse(response.accessToken());
        JwtClaims refreshClaims = jwtService.parseRefreshToken(response.refreshToken());

        UserCredential savedCredential = credentialRepository.findByLogin("new.user")
            .orElseThrow();

        assertThat(accessClaims.userId()).isEqualTo(500L);
        assertThat(accessClaims.subject()).isEqualTo("new.user");
        assertThat(accessClaims.role()).isEqualTo(Role.USER);
        assertThat(accessClaims.tokenType()).isEqualTo(JwtType.ACCESS);

        assertThat(refreshClaims.userId()).isEqualTo(500L);
        assertThat(refreshClaims.subject()).isEqualTo("new.user");
        assertThat(refreshClaims.role()).isEqualTo(Role.USER);
        assertThat(refreshClaims.tokenType()).isEqualTo(JwtType.REFRESH);

        assertThat(savedCredential.getUserId()).isEqualTo(500L);
        assertThat(savedCredential.getLogin()).isEqualTo("new.user");
        assertThat(savedCredential.getRole()).isEqualTo(Role.USER);
        assertThat(savedCredential.getActive()).isTrue();
        assertThat(passwordEncoder.matches("password123", savedCredential.getPasswordHash())).isTrue();
    }

    @Test
    void registerShouldReturnConflictWhenLoginAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "alex.user",
            "password123",
            new UserRequest(
                "Alex",
                "Dudkin",
                "alex.user@test.com",
                LocalDate.of(2000, 1, 1)
            )
        );

        mockMvc.perform(post("/api/v1/auth/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Credential already exists"));

        verifyNoInteractions(userClient);
    }

    @Test
    void loginShouldReturnTokensForActiveCredential() throws Exception {
        LoginRequest request = new LoginRequest("alex.user", "password123");

        String content = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        TokenResponse response = objectMapper.readValue(content, TokenResponse.class);
        JwtClaims accessClaims = jwtService.parse(response.accessToken());
        JwtClaims refreshClaims = jwtService.parseRefreshToken(response.refreshToken());

        assertThat(accessClaims.userId()).isEqualTo(101L);
        assertThat(accessClaims.subject()).isEqualTo("alex.user");
        assertThat(accessClaims.role()).isEqualTo(Role.USER);
        assertThat(accessClaims.tokenType()).isEqualTo(JwtType.ACCESS);

        assertThat(refreshClaims.userId()).isEqualTo(101L);
        assertThat(refreshClaims.subject()).isEqualTo("alex.user");
        assertThat(refreshClaims.role()).isEqualTo(Role.USER);
        assertThat(refreshClaims.tokenType()).isEqualTo(JwtType.REFRESH);
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest("alex.user", "wrong-password");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Authentication failed"))
            .andExpect(jsonPath("$.detail").value("Invalid login or password"));
    }

    @Test
    void loginShouldReturnForbiddenWhenCredentialIsInactive() throws Exception {
        LoginRequest request = new LoginRequest("inactive.user", "inactive123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Credential inactive"))
            .andExpect(jsonPath("$.detail").value("Credential is inactive"));
    }

    @Test
    void refreshShouldReturnTokensForActiveCredential() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(101L, "alex.user", Role.USER);
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        String content = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        TokenResponse response = objectMapper.readValue(content, TokenResponse.class);
        JwtClaims accessClaims = jwtService.parse(response.accessToken());

        assertThat(accessClaims.userId()).isEqualTo(101L);
        assertThat(accessClaims.subject()).isEqualTo("alex.user");
        assertThat(accessClaims.role()).isEqualTo(Role.USER);
        assertThat(accessClaims.tokenType()).isEqualTo(JwtType.ACCESS);
    }

    @Test
    void refreshShouldReturnForbiddenWhenCredentialIsInactive() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(102L, "inactive.user", Role.USER);
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Credential inactive"))
            .andExpect(jsonPath("$.detail").value("Credential is inactive"));
    }

    @Test
    void validateShouldReturnValidResponseForActiveAccessToken() throws Exception {
        String accessToken = jwtService.generateAccessToken(103L, "admin.user", Role.ADMIN);
        ValidateTokenRequest request = new ValidateTokenRequest(accessToken);

        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.userId").value(103L))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.tokenType").value("ACCESS"));
    }

    @Test
    void validateShouldReturnInvalidResponseForBrokenToken() throws Exception {
        ValidateTokenRequest request = new ValidateTokenRequest("broken-token");

        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.userId").isEmpty())
            .andExpect(jsonPath("$.role").isEmpty())
            .andExpect(jsonPath("$.tokenType").isEmpty());
    }

}
