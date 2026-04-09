package org.example.ecommerce.orders.repository.pagination;

import org.example.ecommerce.orders.dto.request.OrderScrollRequest;
import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.repository.OrderRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import static org.example.ecommerce.orders.repository.OrderRepository.Specs.byStatus;
import static org.example.ecommerce.orders.repository.OrderRepository.Specs.byUserId;
import static org.example.ecommerce.orders.repository.OrderRepository.Specs.notDeleted;

@Component
public class OrderSpecificationBuilder {

    public Specification<Order> buildMyOrders(Long userId, OrderScrollRequest request) {
        return base(request).and(byUserId(userId));
    }

    public Specification<Order> buildAllOrders(OrderScrollRequest request) {
        return base(request);
    }

    private Specification<Order> base(OrderScrollRequest request) {
        return Specification
            .where(notDeleted())
            .and(hasStatus(request))
            .and(createdAtFrom(request))
            .and(createdAtTo(request));
    }

    private Specification<Order> hasStatus(OrderScrollRequest request) {
        return request.status() == null
            ? Specification.unrestricted()
            : byStatus(request.status());
    }

    private Specification<Order> createdAtFrom(OrderScrollRequest request) {
        return request.from() == null
            ? Specification.unrestricted()
            : OrderRepository.Specs.createdAtFrom(request.from());
    }

    private Specification<Order> createdAtTo(OrderScrollRequest request) {
        return request.to() == null
            ? Specification.unrestricted()
            : OrderRepository.Specs.createdAtTo(request.to());
    }

}
