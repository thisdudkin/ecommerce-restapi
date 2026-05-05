package org.example.ecommerce.orders.kafka;

import org.example.ecommerce.orders.enums.PaymentStatus;
import org.example.ecommerce.orders.service.OrderPaymentHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentRecordedEventListenerTests {

    @Mock
    private OrderPaymentHandler handler;

    private PaymentRecordedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentRecordedEventListener(handler);
    }

    @Test
    void onPaymentRecordedShouldDelegateToHandler() {
        PaymentRecordedEvent event = new PaymentRecordedEvent(
            "event-1",
            "payment.recorded.v1",
            Instant.parse("2026-04-20T12:00:00Z"),
            "payment-1",
            10L,
            20L,
            PaymentStatus.SUCCESS,
            new BigDecimal("99.99")
        );

        listener.onPaymentRecorded(event);

        verify(handler).handle(event);
    }
}
