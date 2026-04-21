package org.example.ecommerce.gateway;

import org.example.ecommerce.gateway.infrastructure.config.AuthenticationCacheProperties;
import org.example.ecommerce.gateway.infrastructure.config.AuthenticationClientProperties;
import org.example.ecommerce.gateway.infrastructure.config.GatewayRoutesProperties;
import org.example.ecommerce.gateway.infrastructure.config.HttpTimeoutProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;

@SpringBootApplication(
    exclude = ReactiveUserDetailsServiceAutoConfiguration.class
)
@ConfigurationPropertiesScan(
    basePackageClasses = {
        GatewayRoutesProperties.class,
        HttpTimeoutProperties.class,
        AuthenticationClientProperties.class,
        AuthenticationCacheProperties.class
    }
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
