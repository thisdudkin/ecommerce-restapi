package org.example.ecommerce.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class AuthWebClientConfig {

    @Bean
    public WebClient authValidationWebClient(AuthClientProperties properties) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeoutMs())
            .doOnConnected(connection -> connection.addHandlerFirst(
                new ReadTimeoutHandler(properties.readTimeoutMs(), TimeUnit.MILLISECONDS)
            ));

        return WebClient.builder()
            .baseUrl(properties.baseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

}
