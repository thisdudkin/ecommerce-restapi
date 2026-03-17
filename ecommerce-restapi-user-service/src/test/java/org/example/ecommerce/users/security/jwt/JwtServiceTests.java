package org.example.ecommerce.users.security.jwt;

import org.example.ecommerce.users.exception.custom.InvalidJwtException;
import org.example.ecommerce.users.security.config.JwtProperties;
import org.example.ecommerce.users.security.enums.JwtType;
import org.example.ecommerce.users.utils.TestJwtFactory;
import org.example.ecommerce.users.utils.TestJwtKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTests {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
            TestJwtKeys.publicKeyBase64(),
            "authentication-service",
            "authentication-service"
        );

        jwtService = new JwtService(
            properties,
            new JwtPublicKeyProvider(properties)
        );
    }

    @Test
    void parseShouldReturnClaimsForAccessToken() {
        String token = TestJwtFactory.accessToken(
            101L,
            "alex.user",
            "USER",
            "authentication-service"
        );

        VerifiedJwtClaims claims = jwtService.parse(token);

        assertEquals(101L, claims.userId());
        assertEquals("alex.user", claims.subject());
        assertEquals("USER", claims.role());
        assertEquals(JwtType.ACCESS, claims.tokenType());
        assertNull(claims.internal());
        assertNull(claims.serviceName());
        assertNotNull(claims.issuedAt());
        assertNotNull(claims.expiresAt());
    }

    @Test
    void parseShouldThrowWhenTokenIsMalformed() {
        assertThrows(InvalidJwtException.class, () -> jwtService.parse("broken-token"));
    }

    @Test
    void parseShouldThrowWhenIssuerIsInvalid() {
        String token = TestJwtFactory.tokenWithWrongIssuer();

        assertThrows(InvalidJwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void isTrustedInternalServiceShouldReturnTrue() {
        String token = TestJwtFactory.internalToken(
            "authentication-service",
            "authentication-service"
        );

        VerifiedJwtClaims claims = jwtService.parse(token);

        boolean actual = jwtService.isTrustedInternalService(claims);

        assertTrue(actual);
    }

    @Test
    void isTrustedInternalServiceShouldReturnFalseWhenServiceNameDoesNotMatch() {
        String token = TestJwtFactory.internalToken(
            "orders-service",
            "authentication-service"
        );

        VerifiedJwtClaims claims = jwtService.parse(token);

        boolean actual = jwtService.isTrustedInternalService(claims);

        assertFalse(actual);
    }

    @Test
    void isTrustedInternalServiceShouldReturnFalseWhenTokenTypeIsNotInternal() {
        String token = TestJwtFactory.accessToken(
            101L,
            "alex.user",
            "USER",
            "authentication-service"
        );

        VerifiedJwtClaims claims = jwtService.parse(token);

        boolean actual = jwtService.isTrustedInternalService(claims);

        assertFalse(actual);
    }

    @Test
    void isUserAccessTokenShouldReturnTrue() {
        String token = TestJwtFactory.accessToken(
            101L,
            "alex.user",
            "USER",
            "authentication-service"
        );

        VerifiedJwtClaims claims = jwtService.parse(token);

        boolean actual = jwtService.isUserAccessToken(claims);

        assertTrue(actual);
    }

    @Test
    void isUserAccessTokenShouldReturnFalseWhenTokenTypeIsRefresh() {
        String token = TestJwtFactory.refreshToken(
            101L,
            "alex.user",
            "USER",
            "authentication-service"
        );

        VerifiedJwtClaims claims = jwtService.parse(token);

        boolean actual = jwtService.isUserAccessToken(claims);

        assertFalse(actual);
    }

    @Test
    void isUserAccessTokenShouldReturnFalseWhenUserIdIsNull() {
        String token = TestJwtFactory.internalToken(
            "authentication-service",
            "authentication-service"
        );

        VerifiedJwtClaims claims = jwtService.parse(token);

        boolean actual = jwtService.isUserAccessToken(claims);

        assertFalse(actual);
    }
}
