package org.example.ecommerce.payments.infrastructure.internal.adapter;

import org.example.ecommerce.payments.domain.model.Order;
import org.example.ecommerce.payments.infrastructure.internal.client.OrderClient;
import org.example.ecommerce.payments.infrastructure.internal.dto.OrderResponse;
import org.example.ecommerce.payments.infrastructure.internal.dto.OrderStatus;
import org.example.ecommerce.payments.infrastructure.internal.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignOrderReaderTests {

    @Mock
    private OrderClient client;

    private FeignOrderReader reader;

    @BeforeEach
    void setUp() {
        reader = new FeignOrderReader(client);
    }

    @Test
    void getById_shouldMapOrderResponseWithUser() {
        OrderResponse response = new OrderResponse(
            100L,
            new UserResponse(200L, "Alex", "Dudkin", null, "alex@example.com", true, null, null),
            OrderStatus.NEW,
            new BigDecimal("499.99"),
            false,
            null,
            null,
            null
        );
        when(client.getById(100L)).thenReturn(response);

        Order actual = reader.getById(100L);

        assertThat(actual.id()).isEqualTo(100L);
        assertThat(actual.userId()).isEqualTo(200L);
        assertThat(actual.status()).isEqualTo(OrderStatus.NEW);
        assertThat(actual.totalPrice()).isEqualByComparingTo("499.99");
        assertThat(actual.deleted()).isFalse();
    }

    @Test
    void getById_shouldMapOrderResponseWithoutUser() {
        OrderResponse response = new OrderResponse(
            101L,
            null,
            OrderStatus.COMPLETED,
            new BigDecimal("12.34"),
            true,
            null,
            null,
            null
        );
        when(client.getById(101L)).thenReturn(response);

        Order actual = reader.getById(101L);

        assertThat(actual.id()).isEqualTo(101L);
        assertThat(actual.userId()).isNull();
        assertThat(actual.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(actual.totalPrice()).isEqualByComparingTo("12.34");
        assertThat(actual.deleted()).isTrue();
    }
}
