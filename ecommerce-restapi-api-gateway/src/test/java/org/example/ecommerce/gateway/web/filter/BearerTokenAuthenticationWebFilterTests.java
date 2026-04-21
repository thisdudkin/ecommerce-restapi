package org.example.ecommerce.gateway.web.filter;

import org.example.ecommerce.gateway.domain.auth.exception.InvalidTokenException;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticatedUser;
import org.example.ecommerce.gateway.domain.auth.policy.StaticPublicEndpointPolicy;
import org.example.ecommerce.gateway.domain.auth.usecase.RequestAuthenticator;
import org.example.ecommerce.gateway.shared.GatewayHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BearerTokenAuthenticationWebFilterTests {

    @Test
    void filterShouldBypassAuthenticationForPublicEndpoint() {
        RequestAuthenticator authenticator = mock(RequestAuthenticator.class);
        ServerAuthenticationEntryPoint entryPoint = mock(ServerAuthenticationEntryPoint.class);

        BearerTokenAuthenticationWebFilter filter = new BearerTokenAuthenticationWebFilter(
            authenticator,
            new StaticPublicEndpointPolicy(),
            entryPoint
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/auth/login").build()
        );

        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        WebFilterChain chain = webExchange -> {
            forwardedExchange.set(webExchange);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertNotNull(forwardedExchange.get());
        verifyNoInteractions(authenticator, entryPoint);
    }

    @Test
    void filterShouldAuthenticateProtectedRequestAndRewriteHeaders() {
        RequestAuthenticator authenticator = mock(RequestAuthenticator.class);
        ServerAuthenticationEntryPoint entryPoint = mock(ServerAuthenticationEntryPoint.class);

        when(authenticator.authenticate("Bearer token-1"))
            .thenReturn(Mono.just(new AuthenticatedUser(42L, "ROLE_USER", "ACCESS")));

        BearerTokenAuthenticationWebFilter filter = new BearerTokenAuthenticationWebFilter(
            authenticator,
            new StaticPublicEndpointPolicy(),
            entryPoint
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users/42")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-1")
                .header(GatewayHeaders.AUTHENTICATED_USER_ID, "spoofed-id")
                .header(GatewayHeaders.AUTHENTICATED_USER_ROLE, "spoofed-role")
                .header(GatewayHeaders.AUTHENTICATED_TOKEN_TYPE, "spoofed-type")
                .build()
        );

        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        WebFilterChain chain = webExchange -> {
            forwardedRequest.set(webExchange.getRequest());
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertNotNull(forwardedRequest.get());
        assertNull(forwardedRequest.get().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("42", forwardedRequest.get().getHeaders().getFirst(GatewayHeaders.AUTHENTICATED_USER_ID));
        assertEquals("ROLE_USER", forwardedRequest.get().getHeaders().getFirst(GatewayHeaders.AUTHENTICATED_USER_ROLE));
        assertEquals("ACCESS", forwardedRequest.get().getHeaders().getFirst(GatewayHeaders.AUTHENTICATED_TOKEN_TYPE));

        verify(authenticator).authenticate("Bearer token-1");
        verifyNoInteractions(entryPoint);
    }

    @Test
    void filterShouldDelegateToEntryPointWhenAuthenticationFails() {
        RequestAuthenticator authenticator = mock(RequestAuthenticator.class);
        ServerAuthenticationEntryPoint entryPoint = mock(ServerAuthenticationEntryPoint.class);

        when(authenticator.authenticate("Bearer bad-token"))
            .thenReturn(Mono.error(new InvalidTokenException("Token is invalid or expired")));
        when(entryPoint.commence(any(), any(AuthenticationException.class)))
            .thenReturn(Mono.empty());

        BearerTokenAuthenticationWebFilter filter = new BearerTokenAuthenticationWebFilter(
            authenticator,
            new StaticPublicEndpointPolicy(),
            entryPoint
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users/42")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                .build()
        );

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = webExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertFalse(chainCalled.get());
        verify(authenticator).authenticate("Bearer bad-token");
        verify(entryPoint).commence(any(), any(AuthenticationException.class));
    }

}
