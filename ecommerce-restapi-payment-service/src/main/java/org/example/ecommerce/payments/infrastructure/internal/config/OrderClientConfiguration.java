package org.example.ecommerce.payments.infrastructure.internal.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.example.ecommerce.payments.infrastructure.internal.exception.OrderClientErrorDecoder;
import org.example.ecommerce.payments.infrastructure.security.GatewayHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class OrderClientConfiguration {

    @Bean
    public ErrorDecoder orderClientErrorDecoder() {
        return new OrderClientErrorDecoder();
    }

    @Bean
    public RequestInterceptor gatewayHeadersInterceptor() {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletAttributes))
                return;

            HttpServletRequest request = servletAttributes.getRequest();

            forwardIfPresent(request, template, GatewayHeaders.AUTHENTICATED_USER_ID);
            forwardIfPresent(request, template, GatewayHeaders.AUTHENTICATED_USER_ROLE);
            forwardIfPresent(request, template, GatewayHeaders.AUTHENTICATED_TOKEN_TYPE);
        };
    }

    private void forwardIfPresent(HttpServletRequest request,
                                  RequestTemplate template,
                                  String header) {
        String value = request.getHeader(header);
        if (StringUtils.hasText(value))
            template.header(header, value);
    }

}
