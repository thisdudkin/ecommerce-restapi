package org.example.ecommerce.users.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.example.ecommerce.users.exception.custom.InvalidJwtException;
import org.example.ecommerce.users.security.config.JwtProperties;
import org.example.ecommerce.users.security.enums.JwtType;
import org.springframework.stereotype.Service;

import java.security.PublicKey;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final PublicKey publicKey;

    public JwtService(JwtProperties jwtProperties, JwtPublicKeyProvider keyProvider) {
        this.jwtProperties = jwtProperties;
        this.publicKey = keyProvider.publicKey();
    }

    public VerifiedJwtClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String typeValue = claims.get(JwtClaimNames.TYPE, String.class);

            return new VerifiedJwtClaims(
                claims.get(JwtClaimNames.USER_ID, Long.class),
                claims.getSubject(),
                claims.get(JwtClaimNames.ROLE, String.class),
                typeValue == null ? null : JwtType.valueOf(typeValue),
                claims.get(JwtClaimNames.INTERNAL, Boolean.class),
                claims.get(JwtClaimNames.SERVICE_NAME, String.class),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
            );
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new InvalidJwtException("JWT token is invalid", e);
        }
    }

    public boolean isTrustedInternalService(VerifiedJwtClaims claims) {
        return claims.tokenType() == JwtType.INTERNAL
            && Boolean.TRUE.equals(claims.internal())
            && jwtProperties.trustedInternalService().equals(claims.serviceName());
    }

    public boolean isUserAccessToken(VerifiedJwtClaims claims) {
        return claims.tokenType() == JwtType.ACCESS
            && claims.userId() != null
            && claims.role() != null;
    }

}
