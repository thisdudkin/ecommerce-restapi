package org.example.ecommerce.payments.infrastructure.external.adapter;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.example.ecommerce.payments.config.RandomOrgClientProperties;
import org.example.ecommerce.payments.infrastructure.exception.RandomNumberGenerationException;
import org.example.ecommerce.payments.infrastructure.external.client.RandomOrgClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
    classes = RandomOrgRandomNumberProviderIntegrationTests.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "clients.random-org.api-key=test-api-key"
    }
)
@EnableWireMock(
    @ConfigureWireMock(
        name = "random-org",
        baseUrlProperties = "clients.random-org.base-url"
    )
)
class RandomOrgRandomNumberProviderIntegrationTests {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableFeignClients(clients = RandomOrgClient.class)
    @EnableConfigurationProperties(RandomOrgClientProperties.class)
    @Import(RandomOrgRandomNumberProvider.class)
    static class TestApplication {
    }

    @InjectWireMock("random-org")
    private WireMockServer wireMockServer;

    @Autowired
    private RandomNumberProvider randomNumberProvider;

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
    }

    @Test
    void nextIntShouldReturnRandomNumberWhenRandomOrgRespondsSuccessfully() {
        wireMockServer.stubFor(
            post(urlEqualTo("/json-rpc/4/invoke"))
                .willReturn(
                    com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "result": {
                                "random": {
                                  "data": [8],
                                  "completionTime": "2026-04-20 10:00:00Z"
                                },
                                "bitsUsed": 8,
                                "bitsLeft": 249992,
                                "requestsLeft": 999,
                                "advisoryDelay": 0
                              },
                              "id": 1
                            }
                            """)
                )
        );

        int actual = randomNumberProvider.nextInt();

        assertThat(actual).isEqualTo(8);

        wireMockServer.verify(
            1,
            postRequestedFor(urlEqualTo("/json-rpc/4/invoke"))
                .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                .withRequestBody(matchingJsonPath("$.method", equalTo("generateIntegers")))
                .withRequestBody(matchingJsonPath("$.params.apiKey", equalTo("test-api-key")))
                .withRequestBody(matchingJsonPath("$.params.n", equalTo("1")))
        );
    }

    @Test
    void nextIntShouldThrowRandomNumberGenerationExceptionWhenRandomOrgReturnsError() {
        wireMockServer.stubFor(
            post(urlEqualTo("/json-rpc/4/invoke"))
                .willReturn(
                    com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "error": {
                                "code": 401,
                                "message": "Invalid API key",
                                "data": null
                              },
                              "id": 1
                            }
                            """)
                )
        );

        assertThatThrownBy(() -> randomNumberProvider.nextInt())
            .isInstanceOf(RandomNumberGenerationException.class)
            .hasMessage("Random.org error: Code: 401, Message: Invalid API key");
    }

    @Test
    void nextIntShouldThrowRandomNumberGenerationExceptionWhenRandomOrgReturnsEmptyData() {
        wireMockServer.stubFor(
            post(urlEqualTo("/json-rpc/4/invoke"))
                .willReturn(
                    com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "jsonrpc": "2.0",
                              "result": {
                                "random": {
                                  "data": [],
                                  "completionTime": "2026-04-20 10:00:00Z"
                                },
                                "bitsUsed": 8,
                                "bitsLeft": 249992,
                                "requestsLeft": 999,
                                "advisoryDelay": 0
                              },
                              "id": 1
                            }
                            """)
                )
        );

        assertThatThrownBy(() -> randomNumberProvider.nextInt())
            .isInstanceOf(RandomNumberGenerationException.class)
            .hasMessage("Random.org returned empty random data");
    }

}
