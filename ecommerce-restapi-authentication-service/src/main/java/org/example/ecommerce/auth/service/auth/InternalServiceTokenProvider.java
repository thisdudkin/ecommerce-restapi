package org.example.ecommerce.auth.service.auth;

import org.example.ecommerce.auth.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceTokenProvider {

    private final JwtService jwtService;
    private final String serviceName;

    public InternalServiceTokenProvider(JwtService jwtService,
                                        @Value("${spring.application.name}") String serviceName) {
        this.jwtService = jwtService;
        this.serviceName = serviceName;
    }

    public String getToken() {
        return jwtService.generateInternalServiceToken(serviceName);
    }

}
