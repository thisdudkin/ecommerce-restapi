package org.example.ecommerce.auth.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.example.ecommerce.auth.security.config.JwtProperties;
import org.example.ecommerce.auth.security.enums.JwtType;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.exception.InvalidJwtException;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtService(JwtProperties jwtProperties, JwtKeyProvider jwtKeyProvider) {
        this.jwtProperties = jwtProperties;
        this.privateKey = jwtKeyProvider.privateKey();
        this.publicKey = jwtKeyProvider.publicKey();
    }

    public String generateAccessToken(Long userId, String login, Role role) {
        return generateToken(
            userId,
            login,
            role,
            JwtType.ACCESS,
            jwtProperties.accessExpiration()
        );
    }

    public String generateRefreshToken(Long userId, String login, Role role) {
        return generateToken(
            userId,
            login,
            role,
            JwtType.REFRESH,
            jwtProperties.refreshExpiration()
        );
    }

    public String generateInternalServiceToken(String serviceName) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessExpiration());

        return Jwts.builder()
            .subject(serviceName)
            .issuer(jwtProperties.issuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .claim(JwtClaimNames.TYPE, JwtType.INTERNAL.name())
            .claim(JwtClaimNames.INTERNAL, true)
            .claim(JwtClaimNames.SERVICE_NAME, serviceName)
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact();
    }

    public JwtClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String roleValue = claims.get(JwtClaimNames.ROLE, String.class);
            String typeValue = claims.get(JwtClaimNames.TYPE, String.class);

            return new JwtClaims(
                claims.get(JwtClaimNames.USER_ID, Long.class),
                claims.getSubject(),
                roleValue == null ? null : Role.valueOf(roleValue),
                typeValue == null ? null : JwtType.valueOf(typeValue),
                claims.get(JwtClaimNames.INTERNAL, Boolean.class),
                claims.get(JwtClaimNames.SERVICE_NAME, String.class),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
            );
        } catch (ExpiredJwtException e) {
            throw new InvalidJwtException("JWT token is expired", e);
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new InvalidJwtException("JWT token is invalid", e);
        }
    }

    public JwtClaims parseRefreshToken(String token) {
        JwtClaims claims = parse(token);
        if (claims.tokenType() != JwtType.REFRESH)
            throw new InvalidJwtException("Provided token is not a refresh token");
        return claims;
    }

    public JwtClaims parseInternalToken(String token) {
        JwtClaims claims = parse(token);
        if (claims.tokenType() != JwtType.INTERNAL)
            throw new InvalidJwtException("Provided token is not an internal token");
        return claims;
    }

    private String generateToken(Long userId,
                                 String login,
                                 Role role,
                                 JwtType tokenType,
                                 long expirationSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
            .subject(login)
            .issuer(jwtProperties.issuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .claim(JwtClaimNames.USER_ID, userId)
            .claim(JwtClaimNames.ROLE, role.name())
            .claim(JwtClaimNames.TYPE, tokenType.name())
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact();
    }

}
