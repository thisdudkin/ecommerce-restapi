package org.example.ecommerce.payments.web.mapper;

import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.web.dto.request.PaymentV1Request;
import org.example.ecommerce.payments.web.dto.response.PaymentTotalV1Response;
import org.example.ecommerce.payments.web.dto.response.PaymentV1Response;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mapstruct.ReportingPolicy.IGNORE;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    builder = @Builder(disableBuilder = true),
    unmappedSourcePolicy = IGNORE,
    unmappedTargetPolicy = IGNORE
)
public interface PaymentWebMapper {

    @Mapping(target = "userId", source = "userId")
    NewPayment toDomain(PaymentV1Request request, Long userId);

    PaymentV1Response toResponse(Payment payment);

    PaymentTotalV1Response toResponse(BigDecimal amount, Instant from, Instant to);

}
