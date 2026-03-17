package org.example.ecommerce.orders.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.example.ecommerce.orders.enums.OrderStatus;
import org.example.ecommerce.orders.exception.custom.order.EmptyOrderException;
import org.example.ecommerce.orders.exception.custom.order.OrderItemNotFoundInOrderException;
import org.example.ecommerce.orders.exception.custom.order.OrderStateConflictException;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(
            name = "idx_orders_user_id_created_at",
            columnList = "user_id, created_at"
        )
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.NEW;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<OrderItem> orderItems = new ArrayList<>();

    public Order() {
    }

    public Order(Long userId) {
        this.userId = userId;
    }

    public void addItem(Item item, int quantity) {
        validateItem(item);
        validateQuantity(quantity);

        OrderItem existing = findOrderItem(item).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            OrderItem orderItem = new OrderItem(this, item, quantity);
            orderItems.add(orderItem);
        }

        recalculateTotalPrice();
    }

    public void removeItem(Item item) {
        validateItem(item);

        OrderItem existing = findOrderItem(item)
            .orElseThrow(() -> new OrderItemNotFoundInOrderException(item.getId()));

        orderItems.remove(existing);
        existing.detachOrder();

        recalculateTotalPrice();
    }

    public void changeItemQuantity(Item item, int quantity) {
        validateItem(item);
        validateQuantity(quantity);

        OrderItem existing = findOrderItem(item)
            .orElseThrow(() -> new OrderItemNotFoundInOrderException(item.getId()));

        existing.setQuantity(quantity);
        recalculateTotalPrice();
    }

    public void clearItems() {
        for (OrderItem orderItem : orderItems) {
            orderItem.detachOrder();
        }
        orderItems.clear();
        totalPrice = BigDecimal.ZERO;
    }

    public void markPaid() {
        if (orderItems.isEmpty())
            throw new EmptyOrderException();
        if (status != OrderStatus.NEW)
            throw new OrderStateConflictException("Only NEW order can be paid");
        this.status = OrderStatus.PAID;
    }

    public void complete() {
        if (status != OrderStatus.PAID)
            throw new OrderStateConflictException("Only PAID order can be completed");
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        if (status == OrderStatus.COMPLETED)
            throw new OrderStateConflictException("Completed order can't be cancelled");
        this.status = OrderStatus.CANCELLED;
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }

    public boolean isNew() {
        return status == OrderStatus.NEW;
    }

    public boolean isPaid() {
        return status == OrderStatus.PAID;
    }

    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public boolean hasItem(Item item) {
        return findOrderItem(item).isPresent();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;

        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();

        if (thisEffectiveClass != oEffectiveClass)
            return false;

        Order order = (Order) o;
        return getId() != null && Objects.equals(getId(), order.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
            : getClass().hashCode();
    }

    private void validateItem(Item item) {
        if (item == null)
            throw new IllegalArgumentException("Item must not be null");
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }

    private Optional<OrderItem> findOrderItem(Item item) {
        return orderItems.stream()
            .filter(orderItem -> sameItem(orderItem.getItem(), item))
            .findFirst();
    }

    private boolean sameItem(Item left, Item right) {
        if (left == right)
            return true;
        if (left == null || right == null)
            return false;
        if (left.getId() != null && right.getId() != null)
            return Objects.equals(left.getId(), right.getId());
        return false;
    }

    private void recalculateTotalPrice() {
        totalPrice = orderItems.stream()
            .map(orderItem -> orderItem.getItem().getPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
