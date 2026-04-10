package org.example.ecommerce.orders.controller;

import jakarta.validation.Valid;
import org.example.ecommerce.orders.dto.request.OrderAddItemRequest;
import org.example.ecommerce.orders.dto.request.OrderChangeQuantityRequest;
import org.example.ecommerce.orders.dto.request.OrderScrollRequest;
import org.example.ecommerce.orders.dto.request.OrderStatusUpdateRequest;
import org.example.ecommerce.orders.dto.response.OrderPageResponse;
import org.example.ecommerce.orders.dto.response.OrderResponse;
import org.example.ecommerce.orders.dto.response.PaymentResponse;
import org.example.ecommerce.orders.service.OrderOrchestrator;
import org.example.ecommerce.orders.service.OrderPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import static org.example.ecommerce.orders.security.TokenConstants.USER_CLAIM_EXPRESSION;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderOrchestrator orchestrator;
    private final OrderPaymentService paymentService;

    public OrderController(OrderOrchestrator orchestrator, OrderPaymentService paymentService) {
        this.orchestrator = orchestrator;
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                UriComponentsBuilder uriBuilder) {
        OrderResponse response = orchestrator.create(userId);
        return ResponseEntity.created(
            uriBuilder
                .path("/api/v1/orders/{orderId}")
                .buildAndExpand(response.id())
                .toUri()
        ).body(response);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> get(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                             @PathVariable Long orderId) {
        return ResponseEntity.ok(orchestrator.get(userId, orderId));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderPageResponse> getMy(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                   @Valid @ModelAttribute OrderScrollRequest request) {
        return ResponseEntity.ok(orchestrator.getMy(userId, request));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderPageResponse> getAll(@Valid @ModelAttribute OrderScrollRequest request) {
        return ResponseEntity.ok(orchestrator.getAll(request));
    }

    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> addItem(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                 @PathVariable Long orderId,
                                                 @Valid @RequestBody OrderAddItemRequest request) {
        return ResponseEntity.ok(orchestrator.addItem(userId, orderId, request));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> removeItem(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                    @PathVariable Long orderId,
                                                    @PathVariable Long itemId) {
        return ResponseEntity.ok(orchestrator.removeItem(userId, orderId, itemId));
    }

    @PatchMapping("/{orderId}/items")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> changeQuantity(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                        @PathVariable Long orderId,
                                                        @Valid @RequestBody OrderChangeQuantityRequest request) {
        return ResponseEntity.ok(orchestrator.changeQuantity(userId, orderId, request));
    }

    @PostMapping("/{orderId}/payments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentResponse> pay(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                               @PathVariable Long orderId) {
        paymentService.pay(userId, orderId);
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> updateStatus(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                      @PathVariable Long orderId,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orchestrator.updateStatus(userId, orderId, request.status()));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                       @PathVariable Long orderId) {
        orchestrator.delete(userId, orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/restore")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> restore(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                 @PathVariable Long orderId) {
        return ResponseEntity.ok(orchestrator.restore(userId, orderId));
    }

}
