package org.example.ecommerce.auth.service.auth;

import org.example.ecommerce.auth.entity.RefreshToken;
import org.example.ecommerce.auth.repository.RefreshTokenRepository;
import org.example.ecommerce.auth.security.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTests {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private String rawToken;
    private Long userId;
    private Instant expiresAt;

    @BeforeEach
    void setUp() {
        rawToken = "refresh-token-value";
        userId = 101L;
        expiresAt = Instant.now().plusSeconds(3600);
    }

    @Test
    void storeShouldPersistHashedToken() {
        refreshTokenService.store(userId, rawToken, expiresAt);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(expiresAt, saved.getExpiresAt());
        assertFalse(saved.isRevoked());
        assertNotNull(saved.getTokenHash());
        assertNotEquals(rawToken, saved.getTokenHash());
        assertEquals(64, saved.getTokenHash().length());
    }

    @Test
    void validateShouldPassWhenTokenExistsAndMatchesUserAndIsActive() {
        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.of(token));

        assertDoesNotThrow(() -> refreshTokenService.validate(userId, rawToken));
    }

    @Test
    void validateShouldThrowWhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.empty());

        InvalidRefreshTokenException ex = assertThrows(
            InvalidRefreshTokenException.class,
            () -> refreshTokenService.validate(userId, rawToken)
        );

        assertEquals("Refresh token is invalid or revoked", ex.getMessage());
    }

    @Test
    void validateShouldThrowWhenTokenIsRevoked() {
        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(true)
            .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.of(token));

        InvalidRefreshTokenException ex = assertThrows(
            InvalidRefreshTokenException.class,
            () -> refreshTokenService.validate(userId, rawToken)
        );

        assertEquals("Refresh token is invalid or revoked", ex.getMessage());
    }

    @Test
    void validateShouldThrowWhenTokenIsExpired() {
        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().minusSeconds(1))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.of(token));

        InvalidRefreshTokenException ex = assertThrows(
            InvalidRefreshTokenException.class,
            () -> refreshTokenService.validate(userId, rawToken)
        );

        assertEquals("Refresh token is invalid or revoked", ex.getMessage());
    }

    @Test
    void validateShouldThrowWhenTokenBelongsToAnotherUser() {
        RefreshToken token = RefreshToken.builder()
            .userId(999L)
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.of(token));

        InvalidRefreshTokenException ex = assertThrows(
            InvalidRefreshTokenException.class,
            () -> refreshTokenService.validate(userId, rawToken)
        );

        assertEquals("Refresh token is invalid or revoked", ex.getMessage());
    }

    @Test
    void revokeShouldMarkTokenAsRevokedWhenItExists() {
        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.of(token));

        refreshTokenService.revoke(rawToken);

        assertTrue(token.isRevoked());
    }

    @Test
    void revokeShouldDoNothingWhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> refreshTokenService.revoke(rawToken));
    }

    @Test
    void isActiveShouldReturnTrueWhenTokenExistsAndIsNotRevokedAndNotExpired() {
        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.of(token));

        assertTrue(refreshTokenService.isActive(rawToken));
    }

    @Test
    void isActiveShouldReturnFalseWhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.empty());

        assertFalse(refreshTokenService.isActive(rawToken));
    }

    @Test
    void isActiveShouldReturnFalseWhenTokenIsRevoked() {
        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(true)
            .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.of(token));

        assertFalse(refreshTokenService.isActive(rawToken));
    }

    @Test
    void isActiveShouldReturnFalseWhenTokenIsExpired() {
        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().minusSeconds(1))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.of(token));

        assertFalse(refreshTokenService.isActive(rawToken));
    }

    @Test
    void storeShouldPersistExactSha256Hash() {
        refreshTokenService.store(userId, rawToken, expiresAt);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertEquals(hash(rawToken), saved.getTokenHash());
    }

    @Test
    void validateShouldLookupTokenByHashedValue() {
        String expectedHash = hash(rawToken);

        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(expectedHash)
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(expectedHash))
            .thenReturn(Optional.of(token));

        refreshTokenService.validate(userId, rawToken);

        verify(refreshTokenRepository).findByTokenHash(expectedHash);
    }

    @Test
    void revokeShouldPersistUpdatedTokenWhenItExists() {
        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.of(token));

        refreshTokenService.revoke(rawToken);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertTrue(saved.isRevoked());
        assertSame(token, saved);
    }

    @Test
    void revokeShouldNotSaveAnythingWhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
            .thenReturn(Optional.empty());

        refreshTokenService.revoke(rawToken);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void isActiveShouldLookupTokenByHashedValue() {
        String expectedHash = hash(rawToken);

        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(expectedHash)
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(expectedHash))
            .thenReturn(Optional.of(token));

        assertTrue(refreshTokenService.isActive(rawToken));

        verify(refreshTokenRepository).findByTokenHash(expectedHash);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
