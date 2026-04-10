package org.example.ecommerce.payments.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentTotalV1Response(
    BigDecimal amount,
    Instant from,
    Instant to
) { }
