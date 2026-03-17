package org.example.ecommerce.auth.security.jwt;

import org.example.ecommerce.auth.security.config.JwtProperties;
import org.example.ecommerce.auth.utils.TestJwtKeys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtKeyProviderTests {

    @Test
    void shouldLoadKeysFromBase64() {
        JwtProperties properties = new JwtProperties(
            TestJwtKeys.publicKeyBase64(),
            TestJwtKeys.privateKeyBase64(),
            900L,
            2_592_000L,
            "authentication-service"
        );

        JwtKeyProvider provider = new JwtKeyProvider(properties);

        assertArrayEquals(
            TestJwtKeys.publicKey().getEncoded(),
            provider.publicKey().getEncoded()
        );
        assertArrayEquals(
            TestJwtKeys.privateKey().getEncoded(),
            provider.privateKey().getEncoded()
        );
    }

    @Test
    void shouldThrowWhenKeysAreInvalid() {
        JwtProperties properties = new JwtProperties(
            "invalid-public-key",
            "invalid-private-key",
            900L,
            2_592_000L,
            "authentication-service"
        );

        assertThrows(RuntimeException.class, () -> new JwtKeyProvider(properties));
    }
}
