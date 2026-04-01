package org.example.ecommerce.orders.repository;

import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.entity.Order_;
import org.example.ecommerce.orders.enums.OrderStatus;
import org.example.ecommerce.orders.repository.pagination.OrderRepositoryCustom;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order>, OrderRepositoryCustom {

    @Query("""
        select case
                 when count(*) > 0 then true
                 else false
               end
          from Order o
          where o.deleted = false
            and o.id = :orderId
        """)
    boolean exists(Long orderId);

    @Query("""
        select o
          from Order o
          where o.deleted = false
            and o.id = :orderId
        """)
    Optional<Order> findById(Long orderId);

    @Query("""
        select o
          from Order o
          left join fetch o.orderItems oi
          left join fetch oi.item
          where o.id = :orderId
            and o.userId = :userId
            and o.deleted = false
        """)
    Optional<Order> findDetailed(Long orderId, Long userId);

    @Query("""
        select o
          from Order o
          where o.id = :orderId
            and o.userId = :userId
            and o.deleted = true
        """)
    Optional<Order> findDeleted(Long orderId, Long userId);

    @Query("""
        select distinct o
          from Order o
          left join fetch o.orderItems oi
          left join fetch oi.item
          where o.userId = :userId
            and o.id in :orderIds
          order by o.createdAt, o.id
        """)
    List<Order> findPage(Long userId, Iterable<Long> orderIds);

    interface Specs {

        static Specification<Order> notDeleted() {
            return (root, query, builder) ->
                builder.isFalse(root.get(Order_.deleted));
        }

        static Specification<Order> byUserId(Long userId) {
            return (root, query, builder) ->
                builder.equal(root.get(Order_.userId), userId);
        }

        static Specification<Order> byStatus(OrderStatus status) {
            return (root, query, builder) ->
                builder.equal(root.get(Order_.status), status);
        }

        static Specification<Order> createdAtFrom(LocalDateTime from) {
            return (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get(Order_.createdAt), from);
        }

        static Specification<Order> createdAtTo(LocalDateTime to) {
            return (root, query, builder) ->
                builder.lessThanOrEqualTo(root.get(Order_.createdAt), to);
        }

        static Specification<Order> createdAtBetween(LocalDateTime from, LocalDateTime to) {
            return (root, query, builder) ->
                builder.between(root.get(Order_.createdAt), from, to);
        }

    }

}
