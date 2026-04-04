package org.example.ecommerce.gateway.security;

import org.example.ecommerce.gateway.auth.AuthValidateResponse;
import org.example.ecommerce.gateway.auth.CachedAuthValidationService;
import org.example.ecommerce.gateway.exception.MissingBearerTokenException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

public class BearerTokenAuthenticationWebFilter implements WebFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/v1/auth/credentials",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout"
    );

    private final CachedAuthValidationService cachedAuthValidationService;
    private final ServerAuthenticationEntryPoint authenticationEntryPoint;

    public BearerTokenAuthenticationWebFilter(CachedAuthValidationService cachedAuthValidationService,
                                              ServerAuthenticationEntryPoint authenticationEntryPoint) {
        this.cachedAuthValidationService = cachedAuthValidationService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,
                             @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
            .switchIfEmpty(Mono.error(new MissingBearerTokenException("Missing or invalid Authorization header")))
            .flatMap(this::extractBearerToken)
            .flatMap(token -> cachedAuthValidationService.validate(token)
                .flatMap(response -> continueAuthenticated(exchange, chain, token, response)))
            .onErrorResume(AuthenticationException.class,
                ex -> authenticationEntryPoint.commence(exchange, ex));
    }

    private Mono<Void> continueAuthenticated(ServerWebExchange exchange,
                                             WebFilterChain chain,
                                             String token,
                                             AuthValidateResponse response) {
        GatewayPrincipal principal = new GatewayPrincipal(
            response.userId(),
            response.role(),
            response.tokenType()
        );

        GatewayAuthenticationToken authentication = new GatewayAuthenticationToken(principal, token);

        ServerHttpRequest mutatedRequest = exchange.getRequest()
            .mutate()
            .headers(httpHeaders -> {
                httpHeaders.remove(HttpHeaders.AUTHORIZATION);
                httpHeaders.remove(GatewayHeaderNames.AUTHENTICATED_USER_ID);
                httpHeaders.remove(GatewayHeaderNames.AUTHENTICATED_USER_ROLE);
                httpHeaders.remove(GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE);

                httpHeaders.set(GatewayHeaderNames.AUTHENTICATED_USER_ID, String.valueOf(response.userId()));
                httpHeaders.set(GatewayHeaderNames.AUTHENTICATED_USER_ROLE, String.valueOf(response.role()));
                httpHeaders.set(GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE, String.valueOf(response.tokenType()));
            })
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Mono<String> extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return Mono.error(new MissingBearerTokenException("Missing or invalid Authorization header"));
        }

        String token = authorizationHeader.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return Mono.error(new MissingBearerTokenException("Bearer token is empty"));
        }

        return Mono.just(token);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path)
            || path.startsWith("/actuator/health");
    }

}
