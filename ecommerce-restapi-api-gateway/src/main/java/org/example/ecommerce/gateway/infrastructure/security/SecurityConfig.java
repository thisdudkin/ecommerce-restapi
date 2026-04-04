package org.example.ecommerce.gateway.infrastructure.security;

import org.example.ecommerce.gateway.domain.auth.usecase.RequestAuthenticator;
import org.example.ecommerce.gateway.domain.auth.policy.PublicEndpointPolicy;
import org.example.ecommerce.gateway.domain.auth.policy.StaticPublicEndpointPolicy;
import org.example.ecommerce.gateway.web.filter.BearerTokenAuthenticationWebFilter;
import org.example.ecommerce.gateway.web.problem.ProblemDetailsAccessDeniedHandler;
import org.example.ecommerce.gateway.web.problem.ProblemDetailsAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PublicEndpointPolicy publicEndpointPolicy() {
        return new StaticPublicEndpointPolicy();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         RequestAuthenticator requestAuthenticator,
                                                         PublicEndpointPolicy publicEndpointPolicy,
                                                         ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint,
                                                         ProblemDetailsAccessDeniedHandler accessDeniedHandler) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeExchange(exchange -> exchange
                .pathMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/api/v1/auth/credentials",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout"
                ).permitAll()
                .anyExchange().authenticated()
            )
            .addFilterAt(
                new BearerTokenAuthenticationWebFilter(
                    requestAuthenticator,
                    publicEndpointPolicy,
                    authenticationEntryPoint
                ),
                SecurityWebFiltersOrder.AUTHENTICATION
            )
            .build();
    }

}
