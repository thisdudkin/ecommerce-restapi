package org.example.ecommerce.orders.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.example.ecommerce.orders.config.OpenFeignAuthorizationHeadersConfiguration;
import org.example.ecommerce.orders.dto.response.UserResponse;
import org.example.ecommerce.orders.exception.custom.feign.UserNotFoundException;
import org.example.ecommerce.orders.exception.custom.feign.UserServiceUnavailableException;
import org.example.ecommerce.orders.exception.handler.UserClientFallbackFactory;
import org.example.ecommerce.orders.security.GatewayHeaderNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
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
    classes = UserClientIntegrationTests.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.cloud.openfeign.circuitbreaker.enabled=true"
    }
)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EnableWireMock(
    @ConfigureWireMock(
        name = "user-service",
        baseUrlProperties = "clients.user-service.base-url"
    )
)
class UserClientIntegrationTests {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableFeignClients(clients = UserClient.class)
    @Import({
        OpenFeignAuthorizationHeadersConfiguration.class,
        UserClientFallbackFactory.class
    })
    static class TestApplication {
    }

    @InjectWireMock("user-service")
    private WireMockServer wireMockServer;

    @Autowired
    private UserClient userClient;

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getByIdShouldReturnUserWhenUserExists() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/users/1"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        {
                          "id": 1,
                          "name": "John",
                          "surname": "Doe",
                          "birthDate": "1990-05-15",
                          "email": "john.doe@example.com",
                          "active": true,
                          "createdAt": "2024-01-01T10:00:00",
                          "updatedAt": "2024-06-01T12:00:00"
                        }
                        """))
        );

        UserResponse response = userClient.getById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("John");
        assertThat(response.surname()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.active()).isTrue();
        assertThat(response.birthDate()).hasYear(1990).hasMonthValue(5).hasDayOfMonth(15);
    }

    @Test
    void getByIdShouldIgnoreUnknownFieldsWhenResponseHasExtraFields() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/users/2"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        {
                          "id": 2,
                          "name": "Alice",
                          "surname": "Smith",
                          "birthDate": "1995-03-20",
                          "email": "alice@example.com",
                          "active": false,
                          "createdAt": "2024-02-01T09:00:00",
                          "updatedAt": "2024-07-01T11:00:00",
                          "unknownField": "should be ignored"
                        }
                        """))
        );

        UserResponse response = userClient.getById(2L);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.name()).isEqualTo("Alice");
        assertThat(response.active()).isFalse();
    }

    @Test
    void getByIdShouldThrowUserNotFoundExceptionWhenUserNotFound() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/users/99"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.NOT_FOUND.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        {
                          "status": 404,
                          "message": "User not found"
                        }
                        """))
        );

        assertThatThrownBy(() -> userClient.getById(99L))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void getByIdShouldThrowUserServiceUnavailableExceptionWhenServiceUnavailable() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/users/3"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        {
                          "status": 503,
                          "message": "Service unavailable"
                        }
                        """))
        );

        assertThatThrownBy(() -> userClient.getById(3L))
            .isInstanceOf(UserServiceUnavailableException.class)
            .hasMessageContaining("temporarily unavailable");
    }

    @Test
    void getByIdShouldForwardAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayHeaderNames.AUTHENTICATED_USER_ID, "5");
        request.addHeader(GatewayHeaderNames.AUTHENTICATED_USER_ROLE, "USER");
        request.addHeader(GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE, GatewayHeaderNames.ACCESS_TOKEN_TYPE);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/users/5"))
                .withHeader(GatewayHeaderNames.AUTHENTICATED_USER_ID, equalTo("5"))
                .withHeader(GatewayHeaderNames.AUTHENTICATED_USER_ROLE, equalTo("USER"))
                .withHeader(GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE, equalTo(GatewayHeaderNames.ACCESS_TOKEN_TYPE))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""
                        {
                          "id": 5,
                          "name": "Bob",
                          "surname": "Brown",
                          "birthDate": "1988-11-30",
                          "email": "bob@example.com",
                          "active": true,
                          "createdAt": "2024-03-01T08:00:00",
                          "updatedAt": "2024-08-01T10:00:00"
                        }
                        """))
        );

        userClient.getById(5L);

        wireMockServer.verify(
            1,
            getRequestedFor(urlPathEqualTo("/api/v1/users/5"))
                .withHeader(GatewayHeaderNames.AUTHENTICATED_USER_ID, equalTo("5"))
                .withHeader(GatewayHeaderNames.AUTHENTICATED_USER_ROLE, equalTo("USER"))
                .withHeader(GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE, equalTo(GatewayHeaderNames.ACCESS_TOKEN_TYPE))
        );
    }

}
