package org.example.ecommerce.payments.infrastructure.external.adapter;

import org.example.ecommerce.payments.config.RandomOrgClientProperties;
import org.example.ecommerce.payments.infrastructure.exception.RandomNumberGenerationException;
import org.example.ecommerce.payments.infrastructure.external.client.RandomOrgClient;
import org.example.ecommerce.payments.infrastructure.external.dto.RandomOrgRequest;
import org.example.ecommerce.payments.infrastructure.external.dto.RandomOrgResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RandomOrgRandomNumberProviderTests {

    @Mock
    private RandomOrgClient client;

    @Mock
    private RandomOrgClientProperties properties;

    @Captor
    private ArgumentCaptor<RandomOrgRequest> requestCaptor;

    private RandomOrgRandomNumberProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RandomOrgRandomNumberProvider(client, properties);
    }

    @Test
    void nextInt_shouldReturnFirstNumberFromRandomOrgResponse() {
        RandomOrgResponse response = new RandomOrgResponse(
            "2.0",
            new RandomOrgResponse.Result(
                new RandomOrgResponse.Random(List.of(42, 84), "2026-04-20T10:00:00Z"),
                1,
                1,
                1,
                1
            ),
            null,
            1L
        );

        when(properties.apiKey()).thenReturn("api-key");
        when(client.invoke(any(RandomOrgRequest.class))).thenReturn(response);

        int actual = provider.nextInt();

        assertThat(actual).isEqualTo(42);

        verify(client).invoke(requestCaptor.capture());
        RandomOrgRequest request = requestCaptor.getValue();

        assertThat(request.jsonrpc()).isEqualTo("2.0");
        assertThat(request.method()).isEqualTo("generateIntegers");
        assertThat(request.params().apiKey()).isEqualTo("api-key");
        assertThat(request.params().n()).isEqualTo(1);
        assertThat(request.params().min()).isBetween(1, 9);
        assertThat(request.params().max()).isBetween(11, 100);
        assertThat(request.id()).isPositive();
    }

    @Test
    void nextInt_shouldRethrowRandomNumberGenerationException() {
        RandomNumberGenerationException expected = new RandomNumberGenerationException("boom");

        when(properties.apiKey()).thenReturn("api-key");
        when(client.invoke(any(RandomOrgRequest.class))).thenThrow(expected);

        assertThatThrownBy(provider::nextInt)
            .isSameAs(expected);
    }

    @Test
    void nextInt_shouldWrapUnexpectedException() {
        RuntimeException cause = new RuntimeException("network down");

        when(properties.apiKey()).thenReturn("api-key");
        when(client.invoke(any(RandomOrgRequest.class))).thenThrow(cause);

        assertThatThrownBy(provider::nextInt)
            .isInstanceOf(RandomNumberGenerationException.class)
            .hasMessage("Failed to get random number from random.org")
            .hasCause(cause);
    }

    @Test
    void nextInt_shouldThrowWhenResponseIsNull() {
        when(properties.apiKey()).thenReturn("api-key");
        when(client.invoke(any(RandomOrgRequest.class))).thenReturn(null);

        assertThatThrownBy(provider::nextInt)
            .isInstanceOf(RandomNumberGenerationException.class)
            .hasMessage("Random.org response is null");
    }

    @Test
    void nextInt_shouldThrowWhenResponseContainsError() {
        RandomOrgResponse response = new RandomOrgResponse(
            "2.0",
            null,
            new RandomOrgResponse.Error(401, "Bad API key", null),
            1L
        );

        when(properties.apiKey()).thenReturn("api-key");
        when(client.invoke(any(RandomOrgRequest.class))).thenReturn(response);

        assertThatThrownBy(provider::nextInt)
            .isInstanceOf(RandomNumberGenerationException.class)
            .hasMessage("Random.org error: Code: 401, Message: Bad API key");
    }

    @Test
    void nextInt_shouldThrowWhenRandomDataIsMissing() {
        RandomOrgResponse response = new RandomOrgResponse(
            "2.0",
            new RandomOrgResponse.Result(
                new RandomOrgResponse.Random(List.of(), "2026-04-20T10:00:00Z"),
                1,
                1,
                1,
                1
            ),
            null,
            1L
        );

        when(properties.apiKey()).thenReturn("api-key");
        when(client.invoke(any(RandomOrgRequest.class))).thenReturn(response);

        assertThatThrownBy(provider::nextInt)
            .isInstanceOf(RandomNumberGenerationException.class)
            .hasMessage("Random.org returned empty random data");
    }
}
