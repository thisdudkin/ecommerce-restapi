package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.enums.PaymentStatus;
import org.example.ecommerce.orders.kafka.PaymentRecordedEvent;
import org.example.ecommerce.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentHandlerTests {

    @Mock
    private OrderRepository orderRepository;

    private OrderPaymentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderPaymentHandler(orderRepository);
    }

    @Test
    void handleShouldIgnoreNullEvent() {
        handler.handle(null);

        verifyNoInteractions(orderRepository);
    }

    @Test
    void handleShouldIgnoreInvalidEvent() {
        PaymentRecordedEvent event = new PaymentRecordedEvent(
            "event-1",
            "payment.recorded.v1",
            Instant.parse("2026-04-20T12:00:00Z"),
            "payment-1",
            null,
            20L,
            null,
            new BigDecimal("10.00")
        );

        handler.handle(event);

        verifyNoInteractions(orderRepository);
    }

    @Test
    void handleShouldIgnoreEventWhenOrderDoesNotExist() {
        PaymentRecordedEvent event = successEvent();
        when(orderRepository.findById(10L)).thenReturn(Optional.empty());

        handler.handle(event);

        verify(orderRepository).findById(10L);
        verify(orderRepository, never()).flush();
    }

    @Test
    void handleShouldIgnoreFailedPaymentEvent() {
        PaymentRecordedEvent event = new PaymentRecordedEvent(
            "event-2",
            "payment.recorded.v1",
            Instant.parse("2026-04-20T12:00:00Z"),
            "payment-2",
            10L,
            20L,
            PaymentStatus.FAILED,
            new BigDecimal("10.00")
        );
        Order order = mock(Order.class);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handle(event);

        verify(orderRepository).findById(10L);
        verify(order, never()).markPaid();
        verify(orderRepository, never()).flush();
    }

    @Test
    void handleShouldIgnoreDeletedOrder() {
        PaymentRecordedEvent event = successEvent();
        Order order = mock(Order.class);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(order.isDeleted()).thenReturn(true);

        handler.handle(event);

        verify(order, never()).markPaid();
        verify(orderRepository, never()).flush();
    }

    @Test
    void handleShouldIgnoreAlreadyPaidOrder() {
        PaymentRecordedEvent event = successEvent();
        Order order = mock(Order.class);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(order.isDeleted()).thenReturn(false);
        when(order.isPaid()).thenReturn(true);

        handler.handle(event);

        verify(order, never()).markPaid();
        verify(orderRepository, never()).flush();
    }

    @Test
    void handleShouldIgnoreOrderThatIsNotNew() {
        PaymentRecordedEvent event = successEvent();
        Order order = mock(Order.class);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(order.isDeleted()).thenReturn(false);
        when(order.isPaid()).thenReturn(false);
        when(order.isNew()).thenReturn(false);

        handler.handle(event);

        verify(order, never()).markPaid();
        verify(orderRepository, never()).flush();
    }

    @Test
    void handleShouldMarkOrderPaidAndFlushWhenEventIsSuccessful() {
        PaymentRecordedEvent event = successEvent();
        Order order = mock(Order.class);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(order.isDeleted()).thenReturn(false);
        when(order.isPaid()).thenReturn(false);
        when(order.isNew()).thenReturn(true);

        handler.handle(event);

        verify(order).markPaid();
        verify(orderRepository).flush();
    }

    private PaymentRecordedEvent successEvent() {
        return new PaymentRecordedEvent(
            "event-1",
            "payment.recorded.v1",
            Instant.parse("2026-04-20T12:00:00Z"),
            "payment-1",
            10L,
            20L,
            PaymentStatus.SUCCESS,
            new BigDecimal("10.00")
        );
    }
}
