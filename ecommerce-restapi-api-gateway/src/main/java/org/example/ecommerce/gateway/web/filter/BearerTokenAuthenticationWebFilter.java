package org.example.ecommerce.gateway.web.filter;

import org.example.ecommerce.gateway.domain.auth.usecase.RequestAuthenticator;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticatedUser;
import org.example.ecommerce.gateway.domain.auth.policy.PublicEndpointPolicy;
import org.example.ecommerce.gateway.shared.GatewayHeaders;
import org.example.ecommerce.gateway.web.security.GatewayAuthenticationToken;
import org.example.ecommerce.gateway.web.security.GatewayPrincipal;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class BearerTokenAuthenticationWebFilter implements WebFilter {

    private final RequestAuthenticator requestAuthenticator;
    private final PublicEndpointPolicy publicEndpointPolicy;
    private final ServerAuthenticationEntryPoint authenticationEntryPoint;

    public BearerTokenAuthenticationWebFilter(RequestAuthenticator requestAuthenticator,
                                              PublicEndpointPolicy publicEndpointPolicy,
                                              ServerAuthenticationEntryPoint authenticationEntryPoint) {
        this.requestAuthenticator = requestAuthenticator;
        this.publicEndpointPolicy = publicEndpointPolicy;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,
                             @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (publicEndpointPolicy.isPublic(path)) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        return requestAuthenticator.authenticate(authorizationHeader)
            .flatMap(user -> continueAuthenticated(exchange, chain, authorizationHeader, user))
            .onErrorResume(AuthenticationException.class,
                e -> authenticationEntryPoint.commence(exchange, e));
    }

    private Mono<Void> continueAuthenticated(ServerWebExchange exchange,
                                             WebFilterChain chain,
                                             String authorizationHeader,
                                             AuthenticatedUser user) {
        GatewayPrincipal principal = new GatewayPrincipal(
            user.userId(),
            user.role(),
            user.tokenType()
        );

        Authentication authentication = new GatewayAuthenticationToken(principal, authorizationHeader);

        ServerHttpRequest mutatedRequest = exchange.getRequest()
            .mutate()
            .headers(httpHeaders -> {
                httpHeaders.remove(HttpHeaders.AUTHORIZATION);
                httpHeaders.remove(GatewayHeaders.AUTHENTICATED_USER_ID);
                httpHeaders.remove(GatewayHeaders.AUTHENTICATED_USER_ROLE);
                httpHeaders.remove(GatewayHeaders.AUTHENTICATED_TOKEN_TYPE);

                httpHeaders.set(GatewayHeaders.AUTHENTICATED_USER_ID, String.valueOf(user.userId()));
                httpHeaders.set(GatewayHeaders.AUTHENTICATED_USER_ROLE, user.role());
                httpHeaders.set(GatewayHeaders.AUTHENTICATED_TOKEN_TYPE, user.tokenType());
            })
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

}
