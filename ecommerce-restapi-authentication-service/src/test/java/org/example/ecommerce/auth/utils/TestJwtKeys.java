package org.example.ecommerce.auth.utils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public final class TestJwtKeys {

    private static final KeyPair KEY_PAIR = generate();

    private TestJwtKeys() {
    }

    public static String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(
            KEY_PAIR.getPublic().getEncoded()
        );
    }

    public static String privateKeyBase64() {
        return Base64.getEncoder().encodeToString(
            KEY_PAIR.getPrivate().getEncoded()
        );
    }

    public static PublicKey publicKey() {
        return KEY_PAIR.getPublic();
    }

    public static PrivateKey privateKey() {
        return KEY_PAIR.getPrivate();
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
