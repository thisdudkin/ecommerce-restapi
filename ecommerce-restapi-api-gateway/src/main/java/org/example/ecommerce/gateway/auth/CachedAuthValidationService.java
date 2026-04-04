package org.example.ecommerce.gateway.auth;

import org.example.ecommerce.gateway.config.AuthCacheProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class CachedAuthValidationService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final AuthValidationService delegate;
    private final ObjectMapper objectMapper;
    private final AuthCacheProperties cacheProperties;

    public CachedAuthValidationService(ReactiveStringRedisTemplate redisTemplate,
                                       AuthValidationService delegate,
                                       ObjectMapper objectMapper,
                                       AuthCacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.delegate = delegate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
    }

    public Mono<AuthValidateResponse> validate(String token) {
        String cacheKey = buildCacheKey(token);

        return redisTemplate.opsForValue()
            .get(cacheKey)
            .flatMap(cachedJson -> readFromCache(cacheKey, cachedJson))
            .switchIfEmpty(
                delegate.validate(token)
                    .flatMap(response -> cacheResponse(cacheKey, response).thenReturn(response))
            );
    }

    private Mono<AuthValidateResponse> readFromCache(String cacheKey, String cachedJson) {
        return Mono.fromCallable(() -> objectMapper.readValue(cachedJson, AuthValidateResponse.class))
            .onErrorResume(ex ->
                redisTemplate.delete(cacheKey).then(Mono.empty())
            );
    }

    private Mono<Void> cacheResponse(String cacheKey, AuthValidateResponse response) {
        if (!isCacheable(response)) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(response))
            .onErrorMap(Exception.class,
                ex -> new IllegalStateException("Unable to serialize auth validation response", ex))
            .flatMap(json -> redisTemplate.opsForValue()
                .set(cacheKey, json, cacheProperties.ttl()))
            .then();
    }

    private boolean isCacheable(AuthValidateResponse response) {
        return response != null
            && response.valid()
            && response.userId() != null
            && response.role() != null
            && response.tokenType() != null;
    }

    private String buildCacheKey(String token) {
        return cacheProperties.keyPrefix() + sha256(token);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available");
        }
    }

}
