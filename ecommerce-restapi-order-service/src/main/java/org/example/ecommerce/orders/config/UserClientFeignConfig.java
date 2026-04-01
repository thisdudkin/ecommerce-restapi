package org.example.ecommerce.orders.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.example.ecommerce.orders.exception.handler.UserClientErrorDecoder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    public ErrorDecoder userClientErrorDecoder() {
        return new UserClientErrorDecoder();
    }

    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletAttributes))
                return;

            String authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer "))
                template.header(HttpHeaders.AUTHORIZATION, authorization);
        };
    }

}
