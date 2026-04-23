package org.example.ecommerce.payments.infrastructure.kafka.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ecommerce.payments.domain.exception.DomainException;
import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.service.PaymentService;
import org.example.ecommerce.payments.infrastructure.kafka.event.PaymentRequestedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestedEventListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = "${payments.messaging.request-topic-name}")
    public void onPaymentRequested(PaymentRequestedEvent event) {
        log.info(
            "Received payment request event. Event ID: {}, Order ID: {}, User ID: {}, Amount: {}",
            event.eventId(),
            event.orderId(),
            event.userId(),
            event.amount()
        );

        try {
            paymentService.create(
                NewPayment.builder()
                    .orderId(event.orderId())
                    .userId(event.userId())
                    .amount(event.amount())
                    .build()
            );
        } catch (DomainException e) {
            log.info(
                "Payment request event rejected by business rules. Event ID: {}, Order ID: {}, Reason: {}",
                event.eventId(),
                event.orderId(),
                e.getMessage()
            );
        }
    }

}
