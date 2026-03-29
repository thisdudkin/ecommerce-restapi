package org.example.ecommerce.auth.service.auth;

import org.example.ecommerce.auth.entity.RefreshToken;
import org.example.ecommerce.auth.repository.RefreshTokenRepository;
import org.example.ecommerce.auth.security.exception.InvalidRefreshTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class RefreshTokenService {

    private static final String INVALID_REFRESH_TOKEN = "Refresh token is invalid or revoked";

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public void store(Long userId, String rawToken, Instant expiresAt) {
        refreshTokenRepository.save(
            RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(expiresAt)
                .revoked(false)
                .build()
        );
    }

    @Transactional
    public void validate(Long expectedUserId, String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new InvalidRefreshTokenException(INVALID_REFRESH_TOKEN));

        if (refreshToken.isRevoked()
            || refreshToken.getExpiresAt().isBefore(Instant.now())
            || !Objects.equals(refreshToken.getUserId(), expectedUserId))
            throw new InvalidRefreshTokenException(INVALID_REFRESH_TOKEN);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
            .ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
    }

    @Transactional
    public boolean isActive(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hash(rawToken))
            .filter(token -> !token.isRevoked())
            .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
            .isPresent();
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

}
