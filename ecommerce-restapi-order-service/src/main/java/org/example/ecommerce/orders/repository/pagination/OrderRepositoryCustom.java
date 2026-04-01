package org.example.ecommerce.orders.repository.pagination;

import org.example.ecommerce.orders.entity.Order;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepositoryCustom {
    List<Long> findPageIds(Specification<Order> specification, LocalDateTime createdAt, Long id, int size);
}
