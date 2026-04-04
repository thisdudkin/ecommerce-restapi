package org.example.ecommerce.gateway;

import org.example.ecommerce.gateway.config.AuthCacheProperties;
import org.example.ecommerce.gateway.config.AuthClientProperties;
import org.example.ecommerce.gateway.config.GatewayRoutesProperties;
import org.example.ecommerce.gateway.config.HttpTimeoutProperties;
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
        AuthClientProperties.class,
        AuthCacheProperties.class
    }
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
