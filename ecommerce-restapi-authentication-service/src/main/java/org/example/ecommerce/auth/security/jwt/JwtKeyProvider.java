package org.example.ecommerce.auth.security.jwt;

import org.example.ecommerce.auth.security.config.JwtProperties;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtKeyProvider {

    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public JwtKeyProvider(JwtProperties properties) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(properties.publicKey());
            byte[] privateKeyBytes = Base64.getDecoder().decode(properties.privateKey());

            this.publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            this.privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to load JWT RSA keys from Base64", e);
        }
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

}
