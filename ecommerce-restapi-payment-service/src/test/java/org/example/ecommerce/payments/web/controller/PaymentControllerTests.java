package org.example.ecommerce.payments.web.controller;

import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.domain.service.PaymentService;
import org.example.ecommerce.payments.infrastructure.security.CurrentUserProvider;
import org.example.ecommerce.payments.web.dto.response.PaymentTotalV1Response;
import org.example.ecommerce.payments.web.dto.response.PaymentV1Response;
import org.example.ecommerce.payments.web.mapper.PaymentWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTests {

    @Mock
    private PaymentService service;

    @Mock
    private PaymentWebMapper mapper;

    @Mock
    private CurrentUserProvider currentUser;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(service, mapper, currentUser);
    }

    @Test
    void getMyPayments_shouldReturnMappedPaymentsForCurrentUser() {
        Payment payment = new Payment(
            "payment-1",
            10L,
            77L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T10:00:00Z"),
            new BigDecimal("99.99")
        );
        PaymentV1Response response = new PaymentV1Response(
            "payment-1",
            10L,
            77L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T10:00:00Z"),
            new BigDecimal("99.99")
        );

        when(currentUser.userId()).thenReturn(77L);
        when(service.getByUserId(77L)).thenReturn(List.of(payment));
        when(mapper.toResponse(payment)).thenReturn(response);

        ResponseEntity<List<PaymentV1Response>> actual = controller.getMyPayments();

        assertThat(actual.getStatusCode().value()).isEqualTo(200);
        assertThat(actual.getBody()).containsExactly(response);
        verify(service).getByUserId(77L);
    }

    @Test
    void getMyTotal_shouldReturnMappedTotalForCurrentUser() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-01T00:00:00Z");
        PaymentTotalV1Response response = new PaymentTotalV1Response(new BigDecimal("123.45"), from, to);

        when(currentUser.userId()).thenReturn(55L);
        when(service.getTotalSumByDateRange(55L, from, to)).thenReturn(new BigDecimal("123.45"));
        when(mapper.toResponse(new BigDecimal("123.45"), from, to)).thenReturn(response);

        ResponseEntity<PaymentTotalV1Response> actual = controller.getMyTotal(from, to);

        assertThat(actual.getStatusCode().value()).isEqualTo(200);
        assertThat(actual.getBody()).isEqualTo(response);
        verify(service).getTotalSumByDateRange(55L, from, to);
    }

    @Test
    void getByOrderId_shouldReturnMappedPayments() {
        Payment payment = new Payment(
            "payment-2",
            88L,
            66L,
            PaymentStatus.FAILED,
            Instant.parse("2026-04-20T11:00:00Z"),
            new BigDecimal("10.00")
        );
        PaymentV1Response response = new PaymentV1Response(
            "payment-2",
            88L,
            66L,
            PaymentStatus.FAILED,
            Instant.parse("2026-04-20T11:00:00Z"),
            new BigDecimal("10.00")
        );

        when(service.getByOrderId(88L)).thenReturn(List.of(payment));
        when(mapper.toResponse(payment)).thenReturn(response);

        ResponseEntity<List<PaymentV1Response>> actual = controller.getByOrderId(88L);

        assertThat(actual.getStatusCode().value()).isEqualTo(200);
        assertThat(actual.getBody()).containsExactly(response);
    }

    @Test
    void getByStatus_shouldReturnMappedPayments() {
        Payment payment = new Payment(
            "payment-3",
            99L,
            77L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T12:00:00Z"),
            new BigDecimal("200.00")
        );
        PaymentV1Response response = new PaymentV1Response(
            "payment-3",
            99L,
            77L,
            PaymentStatus.SUCCESS,
            Instant.parse("2026-04-20T12:00:00Z"),
            new BigDecimal("200.00")
        );

        when(service.getByStatus(PaymentStatus.SUCCESS)).thenReturn(List.of(payment));
        when(mapper.toResponse(payment)).thenReturn(response);

        ResponseEntity<List<PaymentV1Response>> actual = controller.getByStatus(PaymentStatus.SUCCESS);

        assertThat(actual.getStatusCode().value()).isEqualTo(200);
        assertThat(actual.getBody()).containsExactly(response);
    }

    @Test
    void getTotalForAllUsers_shouldReturnMappedTotal() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant to = Instant.parse("2026-04-01T00:00:00Z");
        PaymentTotalV1Response response = new PaymentTotalV1Response(new BigDecimal("987.65"), from, to);

        when(service.getTotalSumByDateRange(from, to)).thenReturn(new BigDecimal("987.65"));
        when(mapper.toResponse(new BigDecimal("987.65"), from, to)).thenReturn(response);

        ResponseEntity<PaymentTotalV1Response> actual = controller.getTotalForAllUsers(from, to);

        assertThat(actual.getStatusCode().value()).isEqualTo(200);
        assertThat(actual.getBody()).isEqualTo(response);
    }
}
