package org.example.ecommerce.auth.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.example.ecommerce.auth.exception.handler.UserClientErrorDecoder;
import org.example.ecommerce.auth.service.auth.InternalServiceTokenProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(UserClientProperties.class)
public class UserClientFeignConfig {

    @Bean
    public Request.Options requestOptions(UserClientProperties properties) {
        return new Request.Options(
            properties.connectTimeout().toMillis(),
            TimeUnit.MILLISECONDS,
            properties.readTimeout().toMillis(),
            TimeUnit.MILLISECONDS,
            true
        );
    }

    @Bean
    public ErrorDecoder userClientErrorDecoder(ObjectMapper objectMapper) {
        return new UserClientErrorDecoder(objectMapper);
    }

    @Bean
    public RequestInterceptor internalAuthTokenInterceptor(InternalServiceTokenProvider tokenProvider) {
        return requestTemplate -> requestTemplate.header(
            HttpHeaders.AUTHORIZATION,
            "Bearer " + tokenProvider.getToken()
        );
    }

}
