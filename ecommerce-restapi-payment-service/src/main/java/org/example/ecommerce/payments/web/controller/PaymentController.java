package org.example.ecommerce.payments.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.ecommerce.payments.domain.model.PaymentStatus;
import org.example.ecommerce.payments.domain.service.PaymentService;
import org.example.ecommerce.payments.infrastructure.security.CurrentUserProvider;
import org.example.ecommerce.payments.web.dto.response.PaymentTotalV1Response;
import org.example.ecommerce.payments.web.dto.response.PaymentV1Response;
import org.example.ecommerce.payments.web.mapper.PaymentWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;
    private final PaymentWebMapper mapper;
    private final CurrentUserProvider currentUser;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<PaymentV1Response>> getMyPayments() {
        var userId = currentUser.userId();
        var response = service.getByUserId(userId).stream()
            .map(mapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/total")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentTotalV1Response> getMyTotal(@RequestParam Instant from,
                                                             @RequestParam Instant to) {
        var userId = currentUser.userId();
        var total = service.getTotalSumByDateRange(userId, from, to);
        var response = mapper.toResponse(total, from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<List<PaymentV1Response>> getByOrderId(@PathVariable Long orderId) {
        var response = service.getByOrderId(orderId).stream()
            .map(mapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<List<PaymentV1Response>> getByStatus(@RequestParam(value = "q") PaymentStatus status) {
        var response = service.getByStatus(status).stream()
            .map(mapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentTotalV1Response> getTotalForAllUsers(@RequestParam Instant from,
                                                                      @RequestParam Instant to) {
        var total = service.getTotalSumByDateRange(from, to);
        var response = mapper.toResponse(total, from, to);
        return ResponseEntity.ok(response);
    }

}
