package org.example.ecommerce.gateway.infrastructure.auth.cache;

import org.example.ecommerce.gateway.domain.auth.model.AccessToken;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticationValidationResult;
import org.example.ecommerce.gateway.infrastructure.config.AuthenticationCacheProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAuthenticationCacheTests {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getShouldDeserializeCachedValue() throws Exception {
        AuthenticationCacheProperties properties = new AuthenticationCacheProperties(
            Duration.ofSeconds(30),
            "auth:validate:"
        );
        RedisAuthenticationCache cache = new RedisAuthenticationCache(redisTemplate, objectMapper, properties);

        AccessToken token = new AccessToken("token-1");
        AuthenticationValidationResult result = new AuthenticationValidationResult(5L, "ROLE_USER", "ACCESS");
        String cacheKey = expectedCacheKey("token-1");
        String json = objectMapper.writeValueAsString(result);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(Mono.just(json));

        StepVerifier.create(cache.get(token))
            .expectNextMatches(value ->
                value.userId().equals(5L)
                    && value.role().equals("ROLE_USER")
                    && value.tokenType().equals("ACCESS")
            )
            .verifyComplete();

        verify(valueOperations).get(cacheKey);
    }

    @Test
    void getShouldDeleteCorruptedEntryAndReturnEmpty() {
        AuthenticationCacheProperties properties = new AuthenticationCacheProperties(
            Duration.ofSeconds(30),
            "auth:validate:"
        );
        RedisAuthenticationCache cache = new RedisAuthenticationCache(redisTemplate, objectMapper, properties);

        AccessToken token = new AccessToken("token-2");
        String cacheKey = expectedCacheKey("token-2");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(Mono.just("{broken-json"));
        when(redisTemplate.delete(cacheKey)).thenReturn(Mono.just(1L));

        StepVerifier.create(cache.get(token))
            .verifyComplete();

        verify(valueOperations).get(cacheKey);
        verify(redisTemplate).delete(cacheKey);
    }

    @Test
    void putShouldSerializeAndStoreValueUsingHashedKeyAndConfiguredTtl() throws Exception {
        AuthenticationCacheProperties properties = new AuthenticationCacheProperties(
            Duration.ofSeconds(30),
            "auth:validate:"
        );
        RedisAuthenticationCache cache = new RedisAuthenticationCache(redisTemplate, objectMapper, properties);

        AccessToken token = new AccessToken("plain-token");
        AuthenticationValidationResult result = new AuthenticationValidationResult(11L, "ROLE_ADMIN", "ACCESS");
        String cacheKey = expectedCacheKey("plain-token");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(eq(cacheKey), org.mockito.ArgumentMatchers.anyString(), eq(Duration.ofSeconds(30))))
            .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(cache.put(token, result))
            .verifyComplete();

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(cacheKey), jsonCaptor.capture(), eq(Duration.ofSeconds(30)));

        AuthenticationValidationResult stored =
            objectMapper.readValue(jsonCaptor.getValue(), AuthenticationValidationResult.class);

        assertEquals(11L, stored.userId());
        assertEquals("ROLE_ADMIN", stored.role());
        assertEquals("ACCESS", stored.tokenType());
        assertFalse(cacheKey.contains("plain-token"));
    }

    private String expectedCacheKey(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return "auth:validate:" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
