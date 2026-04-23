package org.example.ecommerce.gateway.domain.auth.service;

import org.example.ecommerce.gateway.domain.auth.cache.AuthenticationCache;
import org.example.ecommerce.gateway.domain.auth.exception.InvalidTokenException;
import org.example.ecommerce.gateway.domain.auth.exception.MissingBearerTokenException;
import org.example.ecommerce.gateway.domain.auth.model.AccessToken;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticationValidationResult;
import org.example.ecommerce.gateway.domain.auth.validator.AuthenticationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static reactor.test.StepVerifier.create;

@ExtendWith(MockitoExtension.class)
class AuthenticateRequestServiceTests {

    @Mock
    private AuthenticationValidator authenticationValidator;

    @Mock
    private AuthenticationCache authenticationCache;

    @InjectMocks
    private AuthenticateRequestService service;

    @Test
    void authenticateShouldReturnCachedUserWhenCacheHit() {
        AuthenticationValidationResult cached = new AuthenticationValidationResult(
            1L,
            "ROLE_USER",
            "ACCESS"
        );

        when(authenticationCache.get(eq(new AccessToken("token-1"))))
            .thenReturn(Mono.just(cached));

        create(service.authenticate("Bearer token-1"))
            .expectNextMatches(user ->
                user.userId().equals(1L)
                    && user.role().equals("ROLE_USER")
                    && user.tokenType().equals("ACCESS")
            )
            .verifyComplete();

        verify(authenticationCache).get(eq(new AccessToken("token-1")));
        verifyNoInteractions(authenticationValidator);
        verify(authenticationCache, never()).put(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void authenticateShouldLoadFromValidatorAndCacheWhenCacheMiss() {
        AuthenticationValidationResult validated = new AuthenticationValidationResult(
            7L,
            "ROLE_ADMIN",
            "ACCESS"
        );

        when(authenticationCache.get(eq(new AccessToken("token-2"))))
            .thenReturn(Mono.empty());
        when(authenticationValidator.validate(eq(new AccessToken("token-2"))))
            .thenReturn(Mono.just(validated));
        when(authenticationCache.put(eq(new AccessToken("token-2")), eq(validated)))
            .thenReturn(Mono.empty());

        create(service.authenticate("Bearer token-2"))
            .expectNextMatches(user ->
                user.userId().equals(7L)
                    && user.role().equals("ROLE_ADMIN")
                    && user.tokenType().equals("ACCESS")
            )
            .verifyComplete();

        verify(authenticationCache).get(eq(new AccessToken("token-2")));
        verify(authenticationValidator).validate(eq(new AccessToken("token-2")));
        verify(authenticationCache).put(eq(new AccessToken("token-2")), eq(validated));
    }

    @Test
    void authenticateShouldFailFastWhenAuthorizationHeaderIsInvalid() {
        StepVerifier.create(service.authenticate("Basic token"))
            .expectErrorSatisfies(error -> {
                assertInstanceOf(MissingBearerTokenException.class, error);
                assertEquals("Missing or invalid Authorization header", error.getMessage());
            })
            .verify();

        verifyNoInteractions(authenticationValidator, authenticationCache);
    }

    @Test
    void authenticateShouldPropagateValidatorErrorWithoutCaching() {
        when(authenticationCache.get(eq(new AccessToken("bad-token"))))
            .thenReturn(Mono.empty());
        when(authenticationValidator.validate(eq(new AccessToken("bad-token"))))
            .thenReturn(Mono.error(new InvalidTokenException("Token is invalid")));

        create(service.authenticate("Bearer bad-token"))
            .expectErrorMatches(ex ->
                ex instanceof InvalidTokenException
                    && ex.getMessage().equals("Token is invalid"))
            .verify();

        verify(authenticationCache).get(eq(new AccessToken("bad-token")));
        verify(authenticationValidator).validate(eq(new AccessToken("bad-token")));
        verify(authenticationCache, never()).put(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

}
