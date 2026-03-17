package org.example.ecommerce.auth.security.jwt;

import org.example.ecommerce.auth.security.config.JwtProperties;
import org.example.ecommerce.auth.security.enums.JwtType;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.exception.InvalidJwtException;
import org.example.ecommerce.auth.utils.TestJwtKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    private JwtService jwtService;

    @BeforeEach
    void setup() {
        JwtProperties properties = new JwtProperties(
            TestJwtKeys.publicKeyBase64(),
            TestJwtKeys.privateKeyBase64(),
            900L,
            2_592_000L,
            "authentication-service"
        );

        jwtService = new JwtService(
            properties,
            new JwtKeyProvider(properties)
        );
    }

    @Test
    void generateAccessTokenShouldCreateParsableAccessToken() {
        String token = jwtService.generateAccessToken(101L, "alex.user", Role.USER);

        JwtClaims claims = jwtService.parse(token);

        assertThat(claims.userId()).isEqualTo(101L);
        assertThat(claims.subject()).isEqualTo("alex.user");
        assertThat(claims.role()).isEqualTo(Role.USER);
        assertThat(claims.tokenType()).isEqualTo(JwtType.ACCESS);
        assertThat(claims.internal()).isNull();
        assertThat(claims.serviceName()).isNull();
        assertThat(claims.issuedAt()).isNotNull();
        assertThat(claims.expiresAt()).isAfter(claims.issuedAt());
    }

    @Test
    void generateRefreshTokenShouldCreateParsableRefreshToken() {
        String token = jwtService.generateRefreshToken(202L, "refresh.user", Role.ADMIN);

        JwtClaims claims = jwtService.parseRefreshToken(token);

        assertThat(claims.userId()).isEqualTo(202L);
        assertThat(claims.subject()).isEqualTo("refresh.user");
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
        assertThat(claims.tokenType()).isEqualTo(JwtType.REFRESH);
        assertThat(claims.internal()).isNull();
        assertThat(claims.serviceName()).isNull();
    }

    @Test
    void generateInternalServiceTokenShouldCreateParsableInternalToken() {
        String token = jwtService.generateInternalServiceToken("authentication-service");

        JwtClaims claims = jwtService.parseInternalToken(token);

        assertThat(claims.userId()).isNull();
        assertThat(claims.subject()).isEqualTo("authentication-service");
        assertThat(claims.role()).isNull();
        assertThat(claims.tokenType()).isEqualTo(JwtType.INTERNAL);
        assertThat(claims.internal()).isTrue();
        assertThat(claims.serviceName()).isEqualTo("authentication-service");
    }

    @Test
    void parseRefreshTokenShouldThrowWhenTokenIsNotRefreshToken() {
        String accessToken = jwtService.generateAccessToken(303L, "plain.user", Role.USER);

        assertThatThrownBy(() -> jwtService.parseRefreshToken(accessToken))
            .isInstanceOf(InvalidJwtException.class)
            .hasMessage("Provided token is not a refresh token");
    }

    @Test
    void parseInternalTokenShouldThrowWhenTokenIsNotInternalToken() {
        String accessToken = jwtService.generateAccessToken(404L, "plain.user", Role.USER);

        assertThatThrownBy(() -> jwtService.parseInternalToken(accessToken))
            .isInstanceOf(InvalidJwtException.class)
            .hasMessage("Provided token is not an internal token");
    }

    @Test
    void parseShouldThrowWhenTokenIsMalformed() {
        assertThatThrownBy(() -> jwtService.parse("broken-token"))
            .isInstanceOf(InvalidJwtException.class)
            .hasMessage("JWT token is invalid");
    }

    @Test
    void parseShouldThrowWhenTokenIsExpired() {
        JwtProperties expiredProperties = new JwtProperties(
            TestJwtKeys.publicKeyBase64(),
            TestJwtKeys.privateKeyBase64(),
            -60L,
            -60L,
            "authentication-service"
        );

        JwtService expiredJwtService = new JwtService(
            expiredProperties,
            new JwtKeyProvider(expiredProperties)
        );

        String token = expiredJwtService.generateAccessToken(505L, "expired.user", Role.USER);

        assertThatThrownBy(() -> expiredJwtService.parse(token))
            .isInstanceOf(InvalidJwtException.class)
            .hasMessage("JWT token is expired");
    }

}
