package org.example.ecommerce.payments.web.mapper;

import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.web.dto.request.PaymentV1Request;
import org.example.ecommerce.payments.web.dto.response.PaymentTotalV1Response;
import org.example.ecommerce.payments.web.dto.response.PaymentV1Response;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentWebMapperTests {

    private final PaymentWebMapper mapper = Mappers.getMapper(PaymentWebMapper.class);

    @Test
    void toDomain_shouldMapRequestAndUserId() {
        PaymentV1Request request = new PaymentV1Request(123L, new BigDecimal("45.67"));

        NewPayment actual = mapper.toDomain(request, 999L);

        assertThat(actual.orderId()).isEqualTo(123L);
        assertThat(actual.userId()).isEqualTo(999L);
        assertThat(actual.amount()).isEqualByComparingTo("45.67");
        assertThat(actual.status()).isNull();
        assertThat(actual.timestamp()).isNull();
    }

    @Test
    void toResponse_shouldMapPaymentToResponse() {
        Payment payment = new Payment(
            "payment-1",
            11L,
            22L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T16:00:00Z"),
            new BigDecimal("99.01")
        );

        PaymentV1Response actual = mapper.toResponse(payment);

        assertThat(actual.id()).isEqualTo("payment-1");
        assertThat(actual.orderId()).isEqualTo(11L);
        assertThat(actual.userId()).isEqualTo(22L);
        assertThat(actual.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(actual.timestamp()).isEqualTo(Instant.parse("2026-04-20T16:00:00Z"));
        assertThat(actual.amount()).isEqualByComparingTo("99.01");
    }

    @Test
    void toResponse_shouldMapTotalResponse() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-01T00:00:00Z");

        PaymentTotalV1Response actual = mapper.toResponse(new BigDecimal("555.00"), from, to);

        assertThat(actual.amount()).isEqualByComparingTo("555.00");
        assertThat(actual.from()).isEqualTo(from);
        assertThat(actual.to()).isEqualTo(to);
    }

    @Test
    void toDomain_shouldReturnNull_whenRequestAndUserIdAreNull() {
        NewPayment actual = mapper.toDomain(null, null);

        assertThat(actual).isNull();
    }

    @Test
    void toDomain_shouldMapOnlyUserId_whenRequestIsNull() {
        NewPayment actual = mapper.toDomain(null, 999L);

        assertThat(actual).isNotNull();
        assertThat(actual.orderId()).isNull();
        assertThat(actual.userId()).isEqualTo(999L);
        assertThat(actual.amount()).isNull();
        assertThat(actual.status()).isNull();
        assertThat(actual.timestamp()).isNull();
    }

    @Test
    void toResponse_shouldReturnNull_whenPaymentIsNull() {
        PaymentV1Response actual = mapper.toResponse((Payment) null);

        assertThat(actual).isNull();
    }

    @Test
    void toResponse_shouldReturnNull_whenAmountFromAndToAreAllNull() {
        PaymentTotalV1Response actual = mapper.toResponse(null, null, null);

        assertThat(actual).isNull();
    }
}
