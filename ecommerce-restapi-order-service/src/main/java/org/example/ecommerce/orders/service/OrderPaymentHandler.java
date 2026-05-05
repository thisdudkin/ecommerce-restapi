package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.enums.PaymentStatus;
import org.example.ecommerce.orders.kafka.PaymentRecordedEvent;
import org.example.ecommerce.orders.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPaymentHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentHandler.class);

    private final OrderRepository orderRepository;

    public OrderPaymentHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void handle(PaymentRecordedEvent event) {
        if (event == null || event.orderId() == null || event.status() == null) {
            log.warn("Skip invalid payment event: {}", event);
            return;
        }

        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn("Order not found for payment event. Order ID: {}, Event ID: {}", event.orderId(), event.eventId());
            return;
        }

        if (event.status() == PaymentStatus.FAILED) {
            log.info("Payment failed for Order ID: {}, Payment ID: {}", event.orderId(), event.paymentId());
            return;
        }

        if (order.isDeleted()) {
            log.warn("Skip payment success event for deleted order. Order ID: {}, Event ID: {}", event.orderId(), event.eventId());
            return;
        }

        if (order.isPaid()) {
            log.info("Order is already paid, duplicate event ignored. Order ID: {}, Event ID: {}", event.orderId(), event.eventId());
            return;
        }

        if (!order.isNew()) {
            log.warn("Skip payment success event because order is not NEW. Order ID: {}, Event ID: {}", event.orderId(), event.eventId());
            return;
        }

        order.markPaid();
        orderRepository.flush();

        log.info("Order marked as PAID from payment event. Order ID: {}, Payment ID: {}, Event ID: {}",
            event.orderId(), event.paymentId(), event.eventId());
    }

}
