package org.example.ecommerce.orders.mapper;

import org.example.ecommerce.orders.dto.response.OrderItemResponse;
import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.entity.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.ecommerce.orders.support.TestDataGenerator.item;
import static org.example.ecommerce.orders.support.TestDataGenerator.order;

@SpringJUnitConfig(OrderItemMapperTests.MapperTestConfig.class)
class OrderItemMapperTests {

    @Autowired
    private OrderItemMapper mapper;

    @Configuration
    @ComponentScan(basePackageClasses = OrderItemMapperImpl.class)
    static class MapperTestConfig {
    }

    @Test
    void toResponseMapsAllFieldsAndSubtotal() {
        Item item = item(100L, "Keyboard", BigDecimal.valueOf(12.50));
        Order order = order(200L, 1L);
        OrderItem orderItem = new OrderItem(order, item, 4);

        OrderItemResponse result = mapper.toResponse(orderItem);

        assertThat(result).isNotNull();
        assertThat(result.itemId()).isEqualTo(100L);
        assertThat(result.itemName()).isEqualTo("Keyboard");
        assertThat(result.itemPrice()).isEqualByComparingTo("12.50");
        assertThat(result.quantity()).isEqualTo(4);
        assertThat(result.subtotal()).isEqualByComparingTo("50.00");
    }

    @Test
    void toSubtotalReturnsZeroWhenOrderItemIsNull() {
        assertThat(mapper.toSubtotal(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void toSubtotalReturnsZeroWhenItemIsNull() {
        OrderItem orderItem = new OrderItem(order(200L, 1L), null, 2);

        assertThat(mapper.toSubtotal(orderItem)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void toSubtotalReturnsZeroWhenItemPriceIsNull() {
        Item itemWithNullPrice = new Item("Broken item", null);
        OrderItem orderItem = new OrderItem(order(200L, 1L), itemWithNullPrice, 2);

        assertThat(mapper.toSubtotal(orderItem)).isEqualByComparingTo(BigDecimal.ZERO);
    }

}
