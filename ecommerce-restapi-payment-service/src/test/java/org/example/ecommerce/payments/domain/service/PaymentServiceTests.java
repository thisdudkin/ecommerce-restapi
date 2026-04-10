package org.example.ecommerce.payments.domain.service;

import org.example.ecommerce.payments.domain.model.NewPayment;
import org.example.ecommerce.payments.domain.model.Payment;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.domain.repository.PaymentRepository;
import org.example.ecommerce.payments.infrastructure.external.adapter.RandomNumberProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private RandomNumberProvider randomNumberProvider;

    @Mock
    private PaymentRepository paymentRepository;

    @Captor
    private ArgumentCaptor<NewPayment> newPaymentCaptor;

    @InjectMocks
    private PaymentService service;

    @Test
    void createShouldPersistSuccessfulPayment_whenRandomNumberIsEven() {
        NewPayment input = NewPayment.builder()
            .orderId(11L)
            .userId(22L)
            .amount(BigDecimal.valueOf(149.99))
            .build();

        when(randomNumberProvider.nextInt()).thenReturn(2);
        when(paymentRepository.create(any(NewPayment.class))).thenAnswer(invocation -> {
            NewPayment value = invocation.getArgument(0);
            return new Payment(
                "payment-1",
                value.orderId(),
                value.userId(),
                value.status(),
                value.timestamp(),
                value.amount()
            );
        });

        service.create(input);

        verify(paymentRepository).create(newPaymentCaptor.capture());
        NewPayment persisted = newPaymentCaptor.getValue();

        assertThat(persisted.orderId()).isEqualTo(11L);
        assertThat(persisted.userId()).isEqualTo(22L);
        assertThat(persisted.amount()).isEqualByComparingTo("149.99");
        assertThat(persisted.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(persisted.timestamp()).isNotNull();
    }

    @Test
    void create_shouldPersistFailedPayment_whenRandomNumberIsOdd() {
        NewPayment input = NewPayment.builder()
            .orderId(33L)
            .userId(44L)
            .amount(new BigDecimal("19.50"))
            .build();

        when(randomNumberProvider.nextInt()).thenReturn(3);
        when(paymentRepository.create(any(NewPayment.class))).thenAnswer(invocation -> {
            NewPayment value = invocation.getArgument(0);
            return new Payment(
                "payment-2",
                value.orderId(),
                value.userId(),
                value.status(),
                value.timestamp(),
                value.amount()
            );
        });

        service.create(input);

        verify(paymentRepository).create(newPaymentCaptor.capture());
        NewPayment persisted = newPaymentCaptor.getValue();

        assertThat(persisted.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(persisted.timestamp()).isNotNull();
    }

    @Test
    void getByUserId_shouldDelegateToRepository() {
        List<Payment> expected = List.of(
            new Payment("p-1", 1L, 99L, PaymentStatus.SUCCESS, Instant.parse("2026-01-01T10:00:00Z"), new BigDecimal("10.00"))
        );
        when(paymentRepository.findByUserId(99L)).thenReturn(expected);

        List<Payment> actual = service.getByUserId(99L);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getByOrderId_shouldDelegateToRepository() {
        List<Payment> expected = List.of(
            new Payment("p-2", 77L, 5L, PaymentStatus.FAILED, Instant.parse("2026-01-01T11:00:00Z"), new BigDecimal("25.00"))
        );
        when(paymentRepository.findByOrderId(77L)).thenReturn(expected);

        List<Payment> actual = service.getByOrderId(77L);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getByStatus_shouldDelegateToRepository() {
        List<Payment> expected = List.of(
            new Payment("p-3", 10L, 20L, PaymentStatus.SUCCESS, Instant.parse("2026-01-01T12:00:00Z"), new BigDecimal("30.00"))
        );
        when(paymentRepository.findByStatus(PaymentStatus.SUCCESS)).thenReturn(expected);

        List<Payment> actual = service.getByStatus(PaymentStatus.SUCCESS);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getTotalSumByDateRangeForUser_shouldDelegateToRepository() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        when(paymentRepository.getTotalSumByDateRange(42L, from, to)).thenReturn(new BigDecimal("555.55"));

        BigDecimal actual = service.getTotalSumByDateRange(42L, from, to);

        assertThat(actual).isEqualByComparingTo("555.55");
    }

    @Test
    void getTotalSumByDateRangeForAllUsers_shouldDelegateToRepository() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant to = Instant.parse("2026-04-01T00:00:00Z");
        when(paymentRepository.getTotalSumByDateRange(from, to)).thenReturn(new BigDecimal("777.77"));

        BigDecimal actual = service.getTotalSumByDateRange(from, to);

        assertThat(actual).isEqualByComparingTo("777.77");
    }

}
