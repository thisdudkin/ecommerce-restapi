package org.example.ecommerce.gateway.domain.auth.cache;

import org.example.ecommerce.gateway.domain.auth.model.AccessToken;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticationValidationResult;
import reactor.core.publisher.Mono;

public interface AuthenticationCache {
    Mono<AuthenticationValidationResult> get(AccessToken token);
    Mono<Void> put(AccessToken token, AuthenticationValidationResult result);
}
