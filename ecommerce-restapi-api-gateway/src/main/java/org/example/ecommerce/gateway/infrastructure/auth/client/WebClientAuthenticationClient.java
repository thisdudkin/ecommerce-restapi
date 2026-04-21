package org.example.ecommerce.gateway.infrastructure.auth.client;

import org.example.ecommerce.gateway.domain.auth.validator.AuthenticationValidator;
import org.example.ecommerce.gateway.domain.auth.exception.InvalidTokenException;
import org.example.ecommerce.gateway.domain.auth.exception.TokenValidationUnavailableException;
import org.example.ecommerce.gateway.domain.auth.model.AccessToken;
import org.example.ecommerce.gateway.domain.auth.model.AuthenticationValidationResult;
import org.example.ecommerce.gateway.infrastructure.auth.client.dto.AuthenticationValidateRequest;
import org.example.ecommerce.gateway.infrastructure.auth.client.dto.AuthenticationValidateResponse;
import org.example.ecommerce.gateway.infrastructure.config.AuthenticationClientProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class WebClientAuthenticationClient implements AuthenticationValidator {

    private final WebClient client;
    private final AuthenticationClientProperties properties;

    public WebClientAuthenticationClient(@Qualifier(value = "authValidationWebClient") WebClient client,
                                         AuthenticationClientProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public Mono<AuthenticationValidationResult> validate(AccessToken accessToken) {
        return client.post()
            .uri(properties.validatePath())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new AuthenticationValidateRequest(accessToken.value()))
            .retrieve()
            .bodyToMono(AuthenticationValidateResponse.class)
            .switchIfEmpty(Mono.error(new InvalidTokenException("Token validation returned empty response")))
            .flatMap(this::mapResponse)
            .onErrorMap(WebClientResponseException.Unauthorized.class,
                e -> new InvalidTokenException("Token is invalid or expired"))
            .onErrorMap(WebClientResponseException.Forbidden.class,
                e -> new InvalidTokenException("Token is invalid or expired"))
            .onErrorMap(WebClientResponseException.class,
                e -> new TokenValidationUnavailableException("Authentication service error", e))
            .onErrorMap(e ->
                e instanceof InvalidTokenException || e instanceof TokenValidationUnavailableException
                    ? e
                    : new TokenValidationUnavailableException("Authentication service is unavailable", e)
            );
    }

    private Mono<AuthenticationValidationResult> mapResponse(AuthenticationValidateResponse response) {
        if (!response.valid()) {
            return Mono.error(new InvalidTokenException("Token is invalid"));
        }

        return Mono.just(new AuthenticationValidationResult(
            response.userId(),
            response.role(),
            response.tokenType()
        ));
    }

}
