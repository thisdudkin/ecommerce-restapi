package org.example.ecommerce.gateway.infrastructure.auth.cache;

import org.example.ecommerce.gateway.domain.auth.cache.AuthenticationCache;
import org.example.ecommerce.gateway.domain.auth.model.AccessToken;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticationValidationResult;
import org.example.ecommerce.gateway.infrastructure.config.AuthenticationCacheProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RedisAuthenticationCache implements AuthenticationCache {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthenticationCacheProperties cacheProperties;

    public RedisAuthenticationCache(ReactiveStringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper,
                                    AuthenticationCacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public Mono<AuthenticationValidationResult> get(AccessToken token) {
        String cacheKey = buildCacheKey(token);

        return redisTemplate.opsForValue()
            .get(cacheKey)
            .flatMap(json -> deserialize(cacheKey, json));
    }

    @Override
    public Mono<Void> put(AccessToken token, AuthenticationValidationResult result) {
        String cacheKey = buildCacheKey(token);

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(result))
            .flatMap(json -> redisTemplate.opsForValue().set(cacheKey, json, cacheProperties.ttl()))
            .then();
    }

    private Mono<AuthenticationValidationResult> deserialize(String cacheKey, String json) {
        return Mono.fromCallable(() -> objectMapper.readValue(json, AuthenticationValidationResult.class))
            .onErrorResume(e -> redisTemplate.delete(cacheKey).then(Mono.empty()));
    }

    private String buildCacheKey(AccessToken token) {
        return cacheProperties.keyPrefix() + sha256(token.value());
    }

    private String sha256(String value) {
        try {
            MessageDigest instance = MessageDigest.getInstance("SHA-256");
            byte[] hash = instance.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

}
