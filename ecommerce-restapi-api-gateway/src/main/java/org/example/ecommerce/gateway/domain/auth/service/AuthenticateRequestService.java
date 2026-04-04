package org.example.ecommerce.gateway.domain.auth.service;

import org.example.ecommerce.gateway.domain.auth.usecase.RequestAuthenticator;
import org.example.ecommerce.gateway.domain.auth.cache.AuthenticationCache;
import org.example.ecommerce.gateway.domain.auth.validator.AuthenticationValidator;
import org.example.ecommerce.gateway.domain.auth.model.AccessToken;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticationValidationResult;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticatedUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthenticateRequestService implements RequestAuthenticator {

    private final AuthenticationValidator authenticationValidator;
    private final AuthenticationCache authenticationCache;

    public AuthenticateRequestService(AuthenticationValidator authenticationValidator, AuthenticationCache authenticationCache) {
        this.authenticationValidator = authenticationValidator;
        this.authenticationCache = authenticationCache;
    }

    @Override
    public Mono<AuthenticatedUser> authenticate(String authorizationHeader) {
        AccessToken accessToken = AccessToken.fromAuthorizationHeader(authorizationHeader);

        return authenticationCache.get(accessToken)
            .switchIfEmpty(Mono.defer(() -> loadAndCache(accessToken)))
            .map(AuthenticatedUser::from);
    }

    private Mono<AuthenticationValidationResult> loadAndCache(AccessToken accessToken) {
        return authenticationValidator.validate(accessToken)
            .flatMap(result -> authenticationCache.put(accessToken, result)
                .thenReturn(result));
    }

}
