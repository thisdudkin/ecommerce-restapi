package org.example.ecommerce.auth.security.auth;

import org.example.ecommerce.auth.dto.response.TokenResponse;
import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.security.config.JwtProperties;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.jwt.JwtService;
import org.example.ecommerce.auth.security.principal.AuthUserDetails;
import org.example.ecommerce.auth.service.auth.RefreshTokenService;
import org.example.ecommerce.auth.service.auth.TokenIssuer;
import org.example.ecommerce.auth.utils.TestDataGenerator;
import org.example.ecommerce.auth.utils.TestJwtKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenIssuerTests {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private TokenIssuer tokenIssuer;

    @BeforeEach
    void setUp() {
        tokenIssuer = new TokenIssuer(
            jwtService,
            new JwtProperties(
                TestJwtKeys.publicKeyBase64(),
                TestJwtKeys.privateKeyBase64(),
                900L,
                2_592_000L,
                "authentication-service"
            ),
            refreshTokenService
        );
    }

    @Test
    void issueShouldReturnTokenResponseForCredential() {
        UserCredential credential = TestDataGenerator.userCredential(
            101L,
            "alex.user",
            true,
            Role.USER
        );

        when(jwtService.generateAccessToken(101L, "alex.user", Role.USER))
            .thenReturn("access-token");
        when(jwtService.generateRefreshToken(101L, "alex.user", Role.USER))
            .thenReturn("refresh-token");

        TokenResponse actual = tokenIssuer.issue(credential);

        assertEquals(
            new TokenResponse("access-token", 900L, "refresh-token", 2_592_000L),
            actual
        );

        verify(jwtService).generateAccessToken(101L, "alex.user", Role.USER);
        verify(jwtService).generateRefreshToken(101L, "alex.user", Role.USER);
    }

    @Test
    void issueShouldReturnTokenResponseForUserDetails() {
        AuthUserDetails userDetails = TestDataGenerator.authUserDetails(
            202L,
            "admin.user",
            true,
            Role.ADMIN
        );

        when(jwtService.generateAccessToken(202L, "admin.user", Role.ADMIN))
            .thenReturn("access-token");
        when(jwtService.generateRefreshToken(202L, "admin.user", Role.ADMIN))
            .thenReturn("refresh-token");

        TokenResponse actual = tokenIssuer.issue(userDetails);

        assertEquals(
            new TokenResponse("access-token", 900L, "refresh-token", 2_592_000L),
            actual
        );

        verify(jwtService).generateAccessToken(202L, "admin.user", Role.ADMIN);
        verify(jwtService).generateRefreshToken(202L, "admin.user", Role.ADMIN);
    }

}
