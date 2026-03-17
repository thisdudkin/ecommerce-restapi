package org.example.ecommerce.orders.mapper;

import org.example.ecommerce.orders.dto.response.OrderResponse;
import org.example.ecommerce.orders.dto.response.UserResponse;
import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.ecommerce.orders.support.TestDataGenerator.item;
import static org.example.ecommerce.orders.support.TestDataGenerator.order;
import static org.example.ecommerce.orders.support.TestDataGenerator.userResponse;

@SpringJUnitConfig(OrderMapperTests.MapperTestConfig.class)
class OrderMapperTests {

    @Autowired
    private OrderMapper mapper;

    @Configuration
    @ComponentScan(basePackageClasses = {
        OrderMapperImpl.class,
        OrderItemMapperImpl.class
    })
    static class MapperTestConfig {
    }

    @Test
    void toResponseMapsOrderAndUser() {
        Long userId = 1L;
        Order order = order(200L, userId);
        UserResponse user = userResponse(userId);

        OrderResponse result = mapper.toResponse(order, user);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(order.getId());
        assertThat(result.user()).isEqualTo(user);
        assertThat(result.status()).isEqualTo(order.getStatus());
        assertThat(result.totalPrice()).isEqualByComparingTo(order.getTotalPrice());
        assertThat(result.deleted()).isEqualTo(order.isDeleted());
        assertThat(result.createdAt()).isEqualTo(order.getCreatedAt());
        assertThat(result.updatedAt()).isEqualTo(order.getUpdatedAt());
        assertThat(result.orderItems()).isEmpty();
    }

    @Test
    void toResponseMapsNestedOrderItems() {
        Long userId = 1L;
        Order order = order(200L, userId);
        Item item = item(100L, "Keyboard", BigDecimal.valueOf(15.00));
        order.addItem(item, 3);

        UserResponse user = userResponse(userId);

        OrderResponse result = mapper.toResponse(order, user);

        assertThat(result.orderItems()).hasSize(1);
        assertThat(result.orderItems().getFirst().itemId()).isEqualTo(100L);
        assertThat(result.orderItems().getFirst().itemName()).isEqualTo("Keyboard");
        assertThat(result.orderItems().getFirst().itemPrice()).isEqualByComparingTo("15.00");
        assertThat(result.orderItems().getFirst().quantity()).isEqualTo(3);
        assertThat(result.orderItems().getFirst().subtotal()).isEqualByComparingTo("45.00");
        assertThat(result.totalPrice()).isEqualByComparingTo("45.00");
    }

}
