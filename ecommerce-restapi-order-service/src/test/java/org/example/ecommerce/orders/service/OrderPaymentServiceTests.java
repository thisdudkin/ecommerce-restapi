package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.entity.OrderItem;
import org.example.ecommerce.orders.exception.custom.order.OrderStateConflictException;
import org.example.ecommerce.orders.kafka.PaymentRequestedEvent;
import org.example.ecommerce.orders.kafka.PaymentRequestedEventPublisher;
import org.example.ecommerce.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRequestedEventPublisher publisher;

    @Captor
    private ArgumentCaptor<PaymentRequestedEvent> eventCaptor;

    private OrderPaymentService service;

    @BeforeEach
    void setUp() {
        service = new OrderPaymentService(orderRepository, publisher);
    }

    @Test
    void payShouldPublishPaymentRequestedEventForNewNonEmptyOrder() {
        Order order = mock(Order.class);
        OrderItem orderItem = mock(OrderItem.class);

        when(orderRepository.findDetailed(101L, 202L)).thenReturn(Optional.of(order));
        when(order.isDeleted()).thenReturn(false);
        when(order.getOrderItems()).thenReturn(List.of(orderItem));
        when(order.isNew()).thenReturn(true);
        when(order.getId()).thenReturn(101L);
        when(order.getUserId()).thenReturn(202L);
        when(order.getTotalPrice()).thenReturn(new BigDecimal("44.50"));

        service.pay(202L, 101L);

        verify(publisher).publish(eventCaptor.capture());
        PaymentRequestedEvent event = eventCaptor.getValue();

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.eventType()).isEqualTo(PaymentRequestedEvent.EVENT_TYPE);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.orderId()).isEqualTo(101L);
        assertThat(event.userId()).isEqualTo(202L);
        assertThat(event.amount()).isEqualByComparingTo("44.50");
    }

    @Test
    void payShouldNotPublishAnythingWhenOrderIsNotNew() {
        Order order = mock(Order.class);
        OrderItem orderItem = mock(OrderItem.class);

        when(orderRepository.findDetailed(101L, 202L)).thenReturn(Optional.of(order));
        when(order.isDeleted()).thenReturn(false);
        when(order.getOrderItems()).thenReturn(List.of(orderItem));
        when(order.isNew()).thenReturn(false);

        assertThatThrownBy(() -> service.pay(202L, 101L))
            .isInstanceOf(OrderStateConflictException.class)
            .hasMessage("Only NEW order can be paid");

        verifyNoInteractions(publisher);
    }
}
