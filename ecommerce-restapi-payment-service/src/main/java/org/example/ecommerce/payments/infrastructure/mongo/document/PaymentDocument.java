package org.example.ecommerce.payments.infrastructure.mongo.document;

import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "payments")
@CompoundIndex(name = "idx_payments_user_timestamp", def = "{'user_id': 1, 'timestamp': -1}")
@CompoundIndex(name = "idx_payments_order_id", def = "{'order_id': 1}")
@CompoundIndex(name = "idx_payments_status", def = "{'status': 1}")
@CompoundIndex(name = "idx_payments_outbox_pending", def = "{'published_at': 1, 'timestamp': 1}")
@CompoundIndex(
    name = "uq_payments_success_per_order",
    def = "{'order_id': 1, 'status': 1}",
    unique = true,
    partialFilter = "{'status': 'SUCCESS'}"
)
public record PaymentDocument(
    @Id
    String id,

    @Field(value = "order_id")
    Long orderId,

    @Field(value = "user_id")
    Long userId,

    @Field(value = "status")
    PaymentStatus status,

    @Indexed(direction = IndexDirection.DESCENDING)
    @Field(value = "timestamp")
    Instant timestamp,

    @Field("amount")
    BigDecimal amount,

    @Field(value = "event_id")
    String eventId,

    @Field(value = "published_at")
    Instant publishedAt
) { }
