package org.example.ecommerce.gateway.domain.auth.validator;

import org.example.ecommerce.gateway.domain.auth.model.AccessToken;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticationValidationResult;
import reactor.core.publisher.Mono;

public interface AuthenticationValidator {
    Mono<AuthenticationValidationResult> validate(AccessToken accessToken);
}
