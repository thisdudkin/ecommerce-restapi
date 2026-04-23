package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.exception.custom.order.EmptyOrderException;
import org.example.ecommerce.orders.exception.custom.order.OrderNotFoundException;
import org.example.ecommerce.orders.exception.custom.order.OrderStateConflictException;
import org.example.ecommerce.orders.kafka.PaymentRequestedEvent;
import org.example.ecommerce.orders.kafka.PaymentRequestedEventPublisher;
import org.example.ecommerce.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderPaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRequestedEventPublisher publisher;

    public OrderPaymentService(OrderRepository orderRepository, PaymentRequestedEventPublisher publisher) {
        this.orderRepository = orderRepository;
        this.publisher = publisher;
    }

    public void pay(Long userId, Long orderId) {
        Order order = orderRepository.findDetailed(orderId, userId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.isDeleted())
            throw new OrderStateConflictException("Deleted order can't be paid");

        if (order.getOrderItems().isEmpty())
            throw new EmptyOrderException();

        if (!order.isNew())
            throw new OrderStateConflictException("Only NEW order can be paid");

        publisher.publish(
            new PaymentRequestedEvent(
                UUID.randomUUID().toString(),
                PaymentRequestedEvent.EVENT_TYPE,
                Instant.now(),
                order.getId(),
                order.getUserId(),
                order.getTotalPrice()
            )
        );
    }

}
