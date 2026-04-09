package org.example.ecommerce.orders.repository;

import org.example.ecommerce.orders.dto.request.OrderScrollRequest;
import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.enums.OrderStatus;
import org.example.ecommerce.orders.repository.pagination.OrderSpecificationBuilder;
import org.example.ecommerce.orders.support.AbstractPostgresContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OrderSpecificationBuilder.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(
    scripts = {
        "/sql/cleanup.sql",
        "/sql/order-test-data.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class OrderRepositoryTests extends AbstractPostgresContainer {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSpecificationBuilder orderSpecificationBuilder;

    @Test
    void findDetailedReturnsOnlyOwnNotDeletedOrder() {
        assertThat(orderRepository.findDetailed(200L, 1L))
            .isPresent()
            .get()
            .satisfies(order -> assertThat(order.getOrderItems()).hasSize(1));

        assertThat(orderRepository.findDetailed(200L, 2L)).isEmpty();
        assertThat(orderRepository.findDetailed(203L, 1L)).isEmpty();
    }

    @Test
    void findDeletedReturnsDeletedOrderForConcreteUser() {
        assertThat(orderRepository.findDeleted(203L, 1L)).isPresent();
        assertThat(orderRepository.findDeleted(200L, 1L)).isEmpty();
        assertThat(orderRepository.findDeleted(203L, 2L)).isEmpty();
    }

    @Test
    void findPageIdsAppliesSpecificationAndPageSize() {
        OrderScrollRequest request = new OrderScrollRequest(10, OrderStatus.NEW, null, null, null);
        Specification<Order> specification = orderSpecificationBuilder.buildMyOrders(1L, request);

        List<Long> ids = orderRepository.findPageIds(specification, null, null, 2);

        assertThat(ids).containsExactly(200L, 201L);
    }

    @Test
    void findPageIdsAppliesCursorByCreatedIdAndId() {
        OrderScrollRequest request = new OrderScrollRequest(10, null, null, null, null);
        Specification<Order> specification = orderSpecificationBuilder.buildMyOrders(1L, request);

        List<Long> ids = orderRepository.findPageIds(
            specification,
            LocalDateTime.of(2026, 1, 1, 10, 0),
            200L,
            10
        );

        assertThat(ids).containsExactly(201L, 202L);
    }

}
