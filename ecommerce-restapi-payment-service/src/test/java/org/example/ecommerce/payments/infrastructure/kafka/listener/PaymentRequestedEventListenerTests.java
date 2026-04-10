package org.example.ecommerce.payments.infrastructure.kafka.listener;

import org.example.ecommerce.payments.domain.exception.DomainException;
import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.service.PaymentService;
import org.example.ecommerce.payments.infrastructure.kafka.event.PaymentRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentRequestedEventListenerTests {

    @Mock
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<NewPayment> newPaymentCaptor;

    private PaymentRequestedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentRequestedEventListener(paymentService);
    }

    @Test
    void onPaymentRequested_shouldBuildNewPaymentAndDelegateToService() {
        PaymentRequestedEvent event = new PaymentRequestedEvent(
            "event-1",
            PaymentRequestedEvent.EVENT_TYPE,
            Instant.parse("2026-04-20T12:00:00Z"),
            10L,
            20L,
            new BigDecimal("321.09")
        );

        listener.onPaymentRequested(event);

        verify(paymentService).create(newPaymentCaptor.capture());
        NewPayment actual = newPaymentCaptor.getValue();

        assertThat(actual.orderId()).isEqualTo(10L);
        assertThat(actual.userId()).isEqualTo(20L);
        assertThat(actual.amount()).isEqualByComparingTo("321.09");
        assertThat(actual.status()).isNull();
        assertThat(actual.timestamp()).isNull();
    }

    @Test
    void onPaymentRequested_shouldSwallowDomainException() {
        PaymentRequestedEvent event = new PaymentRequestedEvent(
            "event-2",
            PaymentRequestedEvent.EVENT_TYPE,
            Instant.parse("2026-04-20T12:30:00Z"),
            99L,
            77L,
            new BigDecimal("10.00")
        );

        doThrow(new TestDomainException("business rule rejected"))
            .when(paymentService)
            .create(org.mockito.ArgumentMatchers.any(NewPayment.class));

        assertThatCode(() -> listener.onPaymentRequested(event))
            .doesNotThrowAnyException();
    }

    private static final class TestDomainException extends DomainException {
        private TestDomainException(String message) {
            super(HttpStatus.CONFLICT, "Test domain exception", message);
        }
    }
}
