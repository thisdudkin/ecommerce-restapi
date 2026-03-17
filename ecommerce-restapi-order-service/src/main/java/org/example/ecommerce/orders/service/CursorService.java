package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.dto.request.CursorPayload;
import org.example.ecommerce.orders.exception.custom.pagination.InvalidCursorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class CursorService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final ObjectMapper objectMapper;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public CursorService(ObjectMapper objectMapper,
                         @Value("${pagination.cursor-secret}") String secret) {
        this.objectMapper = objectMapper;
        this.secretKey = new SecretKeySpec(Base64.getDecoder().decode(secret), "AES");
        this.secureRandom = new SecureRandom();
    }

    public String encode(CursorPayload payload) {
        if (payload == null || payload.createdAt() == null || payload.id() == null) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            byte[] plain = objectMapper.writeValueAsBytes(payload);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plain);

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode cursor token", e);
        }
    }

    public CursorPayload decode(String token) {
        if (token == null || token.isBlank()) {
            return new CursorPayload(null, null);
        }

        try {
            byte[] combined = Base64.getUrlDecoder().decode(token);

            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Invalid cursor token");
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plain = cipher.doFinal(encrypted);

            return objectMapper.readValue(plain, CursorPayload.class);
        } catch (Exception e) {
            throw new InvalidCursorException("Invalid cursor token", e);
        }
    }

}
