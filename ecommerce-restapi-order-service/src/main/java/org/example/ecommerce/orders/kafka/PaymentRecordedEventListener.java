package org.example.ecommerce.orders.kafka;

import org.example.ecommerce.orders.service.OrderPaymentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentRecordedEventListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentRecordedEventListener.class);

    private final OrderPaymentHandler handler;

    public PaymentRecordedEventListener(OrderPaymentHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(topics = "${orders.payment-events.topic-name}")
    public void onPaymentRecorded(PaymentRecordedEvent event) {
        log.info("Received payment event. Event ID: {}, Payment ID: {}, Order ID: {}, Status: {}",
            event.eventId(),
            event.paymentId(),
            event.orderId(),
            event.status()
        );
        handler.handle(event);
    }

}
