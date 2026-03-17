package org.example.ecommerce.users.security.jwt;

import org.example.ecommerce.users.security.config.JwtProperties;
import org.example.ecommerce.users.utils.TestJwtKeys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtPublicKeyProviderTests {

    @Test
    void publicKeyShouldReturnDecodedKey() {
        JwtProperties properties = new JwtProperties(
            TestJwtKeys.publicKeyBase64(),
            "authentication-service",
            "authentication-service"
        );

        JwtPublicKeyProvider provider = new JwtPublicKeyProvider(properties);

        assertArrayEquals(
            TestJwtKeys.publicKey().getEncoded(),
            provider.publicKey().getEncoded()
        );
    }

    @Test
    void constructorShouldThrowWhenKeyIsInvalid() {
        JwtProperties properties = new JwtProperties(
            "not-a-valid-base64-key",
            "authentication-service",
            "authentication-service"
        );

        assertThrows(RuntimeException.class, () -> new JwtPublicKeyProvider(properties));
    }
}
