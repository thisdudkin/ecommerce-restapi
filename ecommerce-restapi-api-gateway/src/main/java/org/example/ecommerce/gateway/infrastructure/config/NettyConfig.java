package org.example.ecommerce.gateway.infrastructure.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;

@Configuration
public class NettyConfig {

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer(HttpTimeoutProperties properties) {
        return factory -> factory.addServerCustomizers(httpServer -> customize(httpServer, properties));
    }

    private HttpServer customize(HttpServer httpServer, HttpTimeoutProperties properties) {
        return httpServer
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeoutMs())
            .idleTimeout(Duration.ofMillis(properties.responseTimeoutMs()));
    }

}
