package org.example.ecommerce.gateway.domain.auth.usecase;

import org.example.ecommerce.gateway.domain.auth.model.AuthenticatedUser;
import reactor.core.publisher.Mono;

public interface RequestAuthenticator {
    Mono<AuthenticatedUser> authenticate(String authorizationHeader);
}
