package org.example.ecommerce.gateway.auth;

import org.example.ecommerce.gateway.config.AuthClientProperties;
import org.example.ecommerce.gateway.exception.InvalidTokenException;
import org.example.ecommerce.gateway.exception.TokenValidationServiceUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class AuthValidationService {

    private final WebClient authValidationWebClient;
    private final AuthClientProperties properties;

    public AuthValidationService(WebClient authValidationWebClient, AuthClientProperties properties) {
        this.authValidationWebClient = authValidationWebClient;
        this.properties = properties;
    }

    public Mono<AuthValidateResponse> validate(String token) {
        return authValidationWebClient.post()
            .uri(properties.validatePath())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new AuthValidateRequest(token))
            .retrieve()
            .bodyToMono(AuthValidateResponse.class)
            .onErrorMap(WebClientResponseException.Unauthorized.class,
                ex -> new InvalidTokenException("Token is invalid or expired"))
            .onErrorMap(WebClientResponseException.Forbidden.class,
                ex -> new InvalidTokenException("Token is invalid or expired"))
            .onErrorMap(WebClientResponseException.class,
                ex -> new TokenValidationServiceUnavailableException("Authentication service error"))
            .switchIfEmpty(Mono.error(new InvalidTokenException("Token validation returned empty response")))
            .flatMap(response -> {
                if (!response.valid() || response.userId() == null || response.role() == null || response.tokenType() == null) {
                    return Mono.error(new InvalidTokenException("Token is invalid"));
                }
                return Mono.just(response);
            })
            .onErrorMap(ex -> ex instanceof InvalidTokenException || ex instanceof TokenValidationServiceUnavailableException
                ? ex
                : new TokenValidationServiceUnavailableException("Authentication service is unavailable"));
    }

}
