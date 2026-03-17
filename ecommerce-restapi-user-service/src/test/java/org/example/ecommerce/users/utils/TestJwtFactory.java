package org.example.ecommerce.users.utils;

import io.jsonwebtoken.Jwts;
import org.example.ecommerce.users.security.enums.JwtType;
import org.example.ecommerce.users.security.jwt.JwtClaimNames;

import java.time.Instant;
import java.util.Date;

public final class TestJwtFactory {

    private TestJwtFactory() {
    }

    public static String accessToken(Long userId,
                                     String subject,
                                     String role,
                                     String issuer) {
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(900)))
            .claim(JwtClaimNames.USER_ID, userId)
            .claim(JwtClaimNames.ROLE, role)
            .claim(JwtClaimNames.TYPE, JwtType.ACCESS.name())
            .signWith(TestJwtKeys.privateKey(), Jwts.SIG.RS256)
            .compact();
    }

    public static String refreshToken(Long userId,
                                      String subject,
                                      String role,
                                      String issuer) {
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(900)))
            .claim(JwtClaimNames.USER_ID, userId)
            .claim(JwtClaimNames.ROLE, role)
            .claim(JwtClaimNames.TYPE, JwtType.REFRESH.name())
            .signWith(TestJwtKeys.privateKey(), Jwts.SIG.RS256)
            .compact();
    }

    public static String internalToken(String serviceName,
                                       String issuer) {
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(serviceName)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(900)))
            .claim(JwtClaimNames.TYPE, JwtType.INTERNAL.name())
            .claim(JwtClaimNames.INTERNAL, true)
            .claim(JwtClaimNames.SERVICE_NAME, serviceName)
            .signWith(TestJwtKeys.privateKey(), Jwts.SIG.RS256)
            .compact();
    }

    public static String tokenWithWrongIssuer() {
        Instant now = Instant.now();

        return Jwts.builder()
            .subject("alex.user")
            .issuer("wrong-issuer")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(900)))
            .claim(JwtClaimNames.USER_ID, 101L)
            .claim(JwtClaimNames.ROLE, "USER")
            .claim(JwtClaimNames.TYPE, JwtType.ACCESS.name())
            .signWith(TestJwtKeys.privateKey(), Jwts.SIG.RS256)
            .compact();
    }
}
