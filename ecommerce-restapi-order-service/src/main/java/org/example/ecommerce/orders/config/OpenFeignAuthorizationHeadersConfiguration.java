package org.example.ecommerce.orders.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.example.ecommerce.orders.security.GatewayHeaderNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class OpenFeignAuthorizationHeadersConfiguration {

    @Bean
    public RequestInterceptor authorizationHeadersForwarder() {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
                return;
            }

            HttpServletRequest request = servletAttributes.getRequest();

            forwardIfPresent(request, template, GatewayHeaderNames.AUTHENTICATED_USER_ID);
            forwardIfPresent(request, template, GatewayHeaderNames.AUTHENTICATED_USER_ROLE);
            forwardIfPresent(request, template, GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE);
        };
    }

    private void forwardIfPresent(HttpServletRequest request,
                                  RequestTemplate template,
                                  String headerName) {
        String value = request.getHeader(headerName);
        if (StringUtils.hasText(value))
            template.header(headerName, value);
    }

}
