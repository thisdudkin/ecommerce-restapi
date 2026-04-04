package org.example.ecommerce.gateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class NettyConfig {

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> gatewayNettyCustomizer(HttpTimeoutProperties properties) {
        return factory -> factory.addServerCustomizers(httpServer -> httpServer
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeoutMs())
            .idleTimeout(Duration.ofMillis(properties.responseTimeoutMs()))
        );
    }

}
