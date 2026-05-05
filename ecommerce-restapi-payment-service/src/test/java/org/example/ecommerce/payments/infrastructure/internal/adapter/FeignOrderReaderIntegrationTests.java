package org.example.ecommerce.payments.infrastructure.internal.adapter;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.example.ecommerce.payments.domain.exception.OrderNotFoundException;
import org.example.ecommerce.payments.domain.model.Order;
import org.example.ecommerce.payments.infrastructure.exception.OrderServiceUnavailableException;
import org.example.ecommerce.payments.infrastructure.internal.client.OrderClient;
import org.example.ecommerce.payments.infrastructure.internal.config.OrderClientConfiguration;
import org.example.ecommerce.payments.infrastructure.security.GatewayHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
    classes = FeignOrderReaderIntegrationTests.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@EnableWireMock(
    @ConfigureWireMock(
        name = "order-service",
        baseUrlProperties = "clients.order-service.base-url"
    )
)
class FeignOrderReaderIntegrationTests {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableFeignClients(clients = OrderClient.class)
    @Import({
        OrderClientConfiguration.class,
        FeignOrderReader.class
    })
    static class TestApplication {
    }

    @InjectWireMock("order-service")
    private WireMockServer wireMockServer;

    @Autowired
    private FeignOrderReader feignOrderReader;

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getByIdShouldReturnMappedOrderAndForwardGatewayHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayHeaders.AUTHENTICATED_USER_ID, "15");
        request.addHeader(GatewayHeaders.AUTHENTICATED_USER_ROLE, "ADMIN");
        request.addHeader(GatewayHeaders.AUTHENTICATED_TOKEN_TYPE, GatewayHeaders.ACCESS_TOKEN_TYPE);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/orders/15"))
                .withHeader(GatewayHeaders.AUTHENTICATED_USER_ID, equalTo("15"))
                .withHeader(GatewayHeaders.AUTHENTICATED_USER_ROLE, equalTo("ADMIN"))
                .withHeader(GatewayHeaders.AUTHENTICATED_TOKEN_TYPE, equalTo(GatewayHeaders.ACCESS_TOKEN_TYPE))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        {
                          "id": 15,
                          "user": {
                            "id": 501,
                            "name": "Alex",
                            "surname": "Dudkin",
                            "birthDate": "1994-01-10",
                            "email": "alex@example.com",
                            "active": true,
                            "createdAt": "2026-04-20T10:00:00",
                            "updatedAt": "2026-04-20T10:05:00"
                          },
                          "status": "NEW",
                          "totalPrice": 149.99,
                          "deleted": false,
                          "orderItems": [],
                          "createdAt": "2026-04-20T10:00:00",
                          "updatedAt": "2026-04-20T10:05:00"
                        }
                        """))
        );

        Order actual = feignOrderReader.getById(15L);

        assertThat(actual.id()).isEqualTo(15L);
        assertThat(actual.userId()).isEqualTo(501L);
        assertThat(actual.status().name()).isEqualTo("NEW");
        assertThat(actual.totalPrice()).isEqualByComparingTo("149.99");
        assertThat(actual.deleted()).isFalse();

        wireMockServer.verify(
            1,
            getRequestedFor(urlPathEqualTo("/api/v1/orders/15"))
                .withHeader(GatewayHeaders.AUTHENTICATED_USER_ID, equalTo("15"))
                .withHeader(GatewayHeaders.AUTHENTICATED_USER_ROLE, equalTo("ADMIN"))
                .withHeader(GatewayHeaders.AUTHENTICATED_TOKEN_TYPE, equalTo(GatewayHeaders.ACCESS_TOKEN_TYPE))
        );
    }

    @Test
    void getByIdShouldThrowOrderNotFoundExceptionWhenOrderDoesNotExist() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/orders/999"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.NOT_FOUND.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        {
                          "title": "Order not found"
                        }
                        """))
        );

        assertThatThrownBy(() -> feignOrderReader.getById(999L))
            .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getByIdShouldThrowOrderServiceUnavailableExceptionWhenRemoteServiceIsUnavailable() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/orders/500"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        {
                          "title": "Service unavailable"
                        }
                        """))
        );

        assertThatThrownBy(() -> feignOrderReader.getById(500L))
            .isInstanceOf(OrderServiceUnavailableException.class)
            .hasMessageContaining("temporarily unavailable");
    }
}
