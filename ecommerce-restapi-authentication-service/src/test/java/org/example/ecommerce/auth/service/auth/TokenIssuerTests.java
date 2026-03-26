package org.example.ecommerce.auth.service.auth;

import org.example.ecommerce.auth.dto.response.TokenResponse;
import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.security.config.JwtProperties;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.jwt.JwtService;
import org.example.ecommerce.auth.security.principal.AuthUserDetails;
import org.example.ecommerce.auth.utils.TestDataGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenIssuerTests {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private TokenIssuer tokenIssuer;

    @Test
    void issueShouldReturnTokenPairForCredential() {
        UserCredential credential = TestDataGenerator.userCredential(
            101L,
            "alex.user",
            true,
            Role.USER
        );

        when(jwtService.generateAccessToken(101L, "alex.user", Role.USER)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(101L, "alex.user", Role.USER)).thenReturn("refresh-token");
        when(jwtProperties.accessExpiration()).thenReturn(900L);
        when(jwtProperties.refreshExpiration()).thenReturn(2_592_000L);

        TokenResponse actual = tokenIssuer.issue(credential);

        assertEquals("access-token", actual.accessToken());
        assertEquals(900L, actual.expiresIn());
        assertEquals("refresh-token", actual.refreshToken());
        assertEquals(2_592_000L, actual.refreshExpiresIn());

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenService).store(eq(101L), eq("refresh-token"), instantCaptor.capture());
        assertTrue(instantCaptor.getValue().isAfter(Instant.now().minusSeconds(2_592_100L)));
    }

    @Test
    void issueShouldReturnTokenPairForUserDetails() {
        AuthUserDetails userDetails = TestDataGenerator.authUserDetails(
            101L,
            "alex.user",
            true,
            Role.USER
        );

        when(jwtService.generateAccessToken(101L, "alex.user", Role.USER)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(101L, "alex.user", Role.USER)).thenReturn("refresh-token");
        when(jwtProperties.accessExpiration()).thenReturn(900L);
        when(jwtProperties.refreshExpiration()).thenReturn(2_592_000L);

        TokenResponse actual = tokenIssuer.issue(userDetails);

        assertEquals("access-token", actual.accessToken());
        assertEquals("refresh-token", actual.refreshToken());

        verify(refreshTokenService).store(eq(101L), eq("refresh-token"), any(Instant.class));
    }

    @Test
    void issueShouldGenerateDifferentRefreshTokensOnSubsequentCalls() {
        UserCredential credential = TestDataGenerator.userCredential(
            101L,
            "alex.user",
            true,
            Role.USER
        );

        when(jwtService.generateAccessToken(101L, "alex.user", Role.USER))
            .thenReturn("access-token-1", "access-token-2");
        when(jwtService.generateRefreshToken(101L, "alex.user", Role.USER))
            .thenReturn("refresh-token-1", "refresh-token-2");
        when(jwtProperties.accessExpiration()).thenReturn(900L);
        when(jwtProperties.refreshExpiration()).thenReturn(2_592_000L);

        TokenResponse first = tokenIssuer.issue(credential);
        TokenResponse second = tokenIssuer.issue(credential);

        assertNotEquals(first.refreshToken(), second.refreshToken());
        verify(refreshTokenService, times(2)).store(eq(101L), anyString(), any(Instant.class));
    }

}
