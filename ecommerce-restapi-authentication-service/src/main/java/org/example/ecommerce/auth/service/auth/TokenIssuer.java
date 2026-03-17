package org.example.ecommerce.auth.service.auth;

import org.example.ecommerce.auth.dto.response.TokenResponse;
import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.security.config.JwtProperties;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.jwt.JwtService;
import org.example.ecommerce.auth.security.principal.AuthUserDetails;
import org.springframework.stereotype.Service;

@Service
public class TokenIssuer {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public TokenIssuer(JwtService jwtService, JwtProperties jwtProperties) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public TokenResponse issue(UserCredential credential) {
        return issue(
            credential.getUserId(),
            credential.getLogin(),
            credential.getRole()
        );
    }

    public TokenResponse issue(AuthUserDetails userDetails) {
        return issue(
            userDetails.getUserId(),
            userDetails.getUsername(),
            userDetails.getRole()
        );
    }

    private TokenResponse issue(Long userId, String login, Role role) {
        String accessToken = jwtService.generateAccessToken(
            userId,
            login,
            role
        );

        String refreshToken = jwtService.generateRefreshToken(
            userId,
            login,
            role
        );

        return new TokenResponse(
            accessToken,
            jwtProperties.accessExpiration(),
            refreshToken,
            jwtProperties.refreshExpiration()
        );
    }

}
