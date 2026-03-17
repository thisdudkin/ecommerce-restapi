package org.example.ecommerce.auth.service.auth;

import org.example.ecommerce.auth.client.UserClient;
import org.example.ecommerce.auth.dto.request.LoginRequest;
import org.example.ecommerce.auth.dto.request.RefreshTokenRequest;
import org.example.ecommerce.auth.dto.request.RegisterRequest;
import org.example.ecommerce.auth.dto.request.ValidateTokenRequest;
import org.example.ecommerce.auth.dto.response.TokenResponse;
import org.example.ecommerce.auth.dto.response.UserResponse;
import org.example.ecommerce.auth.dto.response.ValidateTokenResponse;
import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.exception.custom.CredentialAlreadyExistsException;
import org.example.ecommerce.auth.exception.custom.CredentialNotFoundException;
import org.example.ecommerce.auth.exception.custom.InactiveUserCredentialException;
import org.example.ecommerce.auth.repository.UserCredentialRepository;
import org.example.ecommerce.auth.security.enums.JwtType;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.exception.InvalidJwtException;
import org.example.ecommerce.auth.security.jwt.JwtClaims;
import org.example.ecommerce.auth.security.jwt.JwtService;
import org.example.ecommerce.auth.security.principal.AuthUserDetails;
import org.example.ecommerce.auth.service.registration.RegistrationCompensationService;
import org.example.ecommerce.auth.utils.TestDataGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserClient userClient;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private RegistrationCompensationService registrationCompensationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldReturnTokensWhenCredentialCreated() {
        RegisterRequest request = TestDataGenerator.registerRequest();
        UserResponse createdUser = TestDataGenerator.userResponse(101L);
        UserCredential savedCredential = TestDataGenerator.userCredential(
            101L,
            request.login(),
            true,
            Role.USER
        );
        TokenResponse expected = TestDataGenerator.tokenResponse();

        when(credentialRepository.existsByLogin(request.login())).thenReturn(false);
        when(userClient.create(request.user())).thenReturn(createdUser);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(credentialRepository.saveAndFlush(any(UserCredential.class))).thenReturn(savedCredential);
        when(tokenIssuer.issue(savedCredential)).thenReturn(expected);

        TokenResponse actual = authService.register(request);

        assertEquals(expected, actual);

        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
        verify(credentialRepository).saveAndFlush(captor.capture());

        UserCredential toSave = captor.getValue();
        assertEquals(101L, toSave.getUserId());
        assertEquals(request.login(), toSave.getLogin());
        assertEquals("encoded-password", toSave.getPasswordHash());
        assertEquals(Role.USER, toSave.getRole());
        assertTrue(toSave.getActive());

        verify(credentialRepository).existsByLogin(request.login());
        verify(userClient).create(request.user());
        verify(passwordEncoder).encode(request.password());
        verify(tokenIssuer).issue(savedCredential);
        verifyNoInteractions(registrationCompensationService);
    }

    @Test
    void registerShouldThrowWhenCredentialAlreadyExists() {
        RegisterRequest request = TestDataGenerator.registerRequest();

        when(credentialRepository.existsByLogin(request.login())).thenReturn(true);

        assertThrows(
            CredentialAlreadyExistsException.class,
            () -> authService.register(request)
        );

        verify(credentialRepository).existsByLogin(request.login());
        verifyNoInteractions(
            userClient,
            passwordEncoder,
            tokenIssuer,
            authenticationManager,
            registrationCompensationService
        );
    }

    @Test
    void registerShouldRollbackAndThrowCredentialAlreadyExistsWhenConstraintViolationOccurs() {
        RegisterRequest request = TestDataGenerator.registerRequest();
        UserResponse createdUser = TestDataGenerator.userResponse(202L);

        when(credentialRepository.existsByLogin(request.login())).thenReturn(false);
        when(userClient.create(request.user())).thenReturn(createdUser);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(credentialRepository.saveAndFlush(any(UserCredential.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(
            CredentialAlreadyExistsException.class,
            () -> authService.register(request)
        );

        verify(registrationCompensationService)
            .rollbackUserCreation(eq(createdUser.id()), any(DataIntegrityViolationException.class));
        verify(tokenIssuer, never()).issue(any(UserCredential.class));
    }

    @Test
    void loginShouldAuthenticateAndReturnTokens() {
        LoginRequest request = new LoginRequest("alex.user", "password123");
        AuthUserDetails principal = TestDataGenerator.authUserDetails(
            101L,
            "alex.user",
            true,
            Role.USER
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );
        TokenResponse expected = TestDataGenerator.tokenResponse();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(tokenIssuer.issue(principal)).thenReturn(expected);

        TokenResponse actual = authService.login(request);

        assertEquals(expected, actual);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
            ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("alex.user", captor.getValue().getPrincipal());
        assertEquals("password123", captor.getValue().getCredentials());
        verify(tokenIssuer).issue(principal);
    }

    @Test
    void refreshShouldReturnTokensWhenCredentialIsActive() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        JwtClaims claims = new JwtClaims(
            101L,
            "alex.user",
            Role.USER,
            JwtType.REFRESH,
            null,
            null,
            Instant.now(),
            Instant.now().plusSeconds(60)
        );
        UserCredential credential = TestDataGenerator.userCredential(
            101L,
            "alex.user",
            true,
            Role.USER
        );
        TokenResponse expected = TestDataGenerator.tokenResponse();

        when(jwtService.parseRefreshToken("refresh-token")).thenReturn(claims);
        when(credentialRepository.findByUserId(101L)).thenReturn(Optional.of(credential));
        when(tokenIssuer.issue(credential)).thenReturn(expected);

        TokenResponse actual = authService.refresh(request);

        assertEquals(expected, actual);
        verify(jwtService).parseRefreshToken("refresh-token");
        verify(credentialRepository).findByUserId(101L);
        verify(tokenIssuer).issue(credential);
    }

    @Test
    void refreshShouldThrowWhenCredentialNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        JwtClaims claims = new JwtClaims(
            303L,
            "missing.user",
            Role.USER,
            JwtType.REFRESH,
            null,
            null,
            Instant.now(),
            Instant.now().plusSeconds(60)
        );

        when(jwtService.parseRefreshToken("refresh-token")).thenReturn(claims);
        when(credentialRepository.findByUserId(303L)).thenReturn(Optional.empty());

        assertThrows(
            CredentialNotFoundException.class,
            () -> authService.refresh(request)
        );

        verify(jwtService).parseRefreshToken("refresh-token");
        verify(credentialRepository).findByUserId(303L);
        verify(tokenIssuer, never()).issue(any(UserCredential.class));
    }

    @Test
    void refreshShouldThrowWhenCredentialIsInactive() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        JwtClaims claims = new JwtClaims(
            404L,
            "inactive.user",
            Role.USER,
            JwtType.REFRESH,
            null,
            null,
            Instant.now(),
            Instant.now().plusSeconds(60)
        );
        UserCredential credential = TestDataGenerator.userCredential(
            404L,
            "inactive.user",
            false,
            Role.USER
        );

        when(jwtService.parseRefreshToken("refresh-token")).thenReturn(claims);
        when(credentialRepository.findByUserId(404L)).thenReturn(Optional.of(credential));

        assertThrows(
            InactiveUserCredentialException.class,
            () -> authService.refresh(request)
        );

        verify(jwtService).parseRefreshToken("refresh-token");
        verify(credentialRepository).findByUserId(404L);
        verify(tokenIssuer, never()).issue(any(UserCredential.class));
    }

    @Test
    void validateShouldReturnValidResponseWhenTokenBelongsToActiveCredential() {
        ValidateTokenRequest request = new ValidateTokenRequest("access-token");
        JwtClaims claims = new JwtClaims(
            101L,
            "alex.user",
            Role.USER,
            JwtType.ACCESS,
            null,
            null,
            Instant.now(),
            Instant.now().plusSeconds(60)
        );
        UserCredential credential = TestDataGenerator.userCredential(
            101L,
            "alex.user",
            true,
            Role.USER
        );

        when(jwtService.parse("access-token")).thenReturn(claims);
        when(credentialRepository.findByUserId(101L)).thenReturn(Optional.of(credential));

        ValidateTokenResponse actual = authService.validate(request);

        assertTrue(actual.valid());
        assertEquals(101L, actual.userId());
        assertEquals(Role.USER, actual.role());
        assertEquals(JwtType.ACCESS, actual.tokenType());
    }

    @Test
    void validateShouldReturnInvalidResponseWhenTokenIsInvalid() {
        ValidateTokenRequest request = new ValidateTokenRequest("broken-token");

        when(jwtService.parse("broken-token"))
            .thenThrow(new InvalidJwtException("JWT token is invalid"));

        ValidateTokenResponse actual = authService.validate(request);

        assertFalse(actual.valid());
        assertNull(actual.userId());
        assertNull(actual.role());
        assertNull(actual.tokenType());

        verify(jwtService).parse("broken-token");
        verify(credentialRepository, never()).findByUserId(anyLong());
    }

}
