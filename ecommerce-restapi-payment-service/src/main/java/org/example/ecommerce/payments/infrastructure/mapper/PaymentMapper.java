package org.example.ecommerce.payments.infrastructure.mapper;

import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentOutboxRecord;
import org.example.ecommerce.payments.infrastructure.mongo.document.PaymentDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING
)
public interface PaymentMapper {
    Payment toDomain(PaymentDocument document);

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "occurredAt", source = "timestamp")
    PaymentOutboxRecord toOutboxRecord(PaymentDocument document);
}
