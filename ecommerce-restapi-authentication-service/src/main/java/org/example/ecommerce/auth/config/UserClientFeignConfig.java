package org.example.ecommerce.auth.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.example.ecommerce.auth.exception.handler.UserClientErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class UserClientFeignConfig {

    @Bean
    public ErrorDecoder userClientErrorDecoder(ObjectMapper objectMapper) {
        return new UserClientErrorDecoder(objectMapper);
    }

    @Bean
    public RequestInterceptor internalAuthTokenInterceptor() {
        return template -> {
            template.removeHeader(GatewayHeaderNames.AUTHENTICATED_USER_ID);
            template.removeHeader(GatewayHeaderNames.AUTHENTICATED_USER_ROLE);
            template.removeHeader(GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE);

            template.header(
                GatewayHeaderNames.AUTHENTICATED_USER_ROLE,
                GatewayHeaderNames.INTERNAL_SERVICE_ROLE
            );

            template.header(
                GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE,
                GatewayHeaderNames.INTERNAL_TOKEN_TYPE
            );
        };
    }

}
