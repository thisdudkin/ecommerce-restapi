package org.example.ecommerce.orders.security.jwt;

import org.example.ecommerce.orders.security.config.JwtProperties;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtPublicKeyProvider {

    private final PublicKey publicKey;

    public JwtPublicKeyProvider(JwtProperties properties) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(properties.publicKey());

            this.publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to load JWT RSA public key from Base64", e);
        }
    }

    public PublicKey publicKey() {
        return publicKey;
    }

}
