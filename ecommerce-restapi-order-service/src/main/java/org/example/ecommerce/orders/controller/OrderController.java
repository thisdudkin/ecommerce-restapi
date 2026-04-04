package org.example.ecommerce.orders.controller;

import jakarta.validation.Valid;
import org.example.ecommerce.orders.dto.request.OrderAddItemRequest;
import org.example.ecommerce.orders.dto.request.OrderChangeQuantityRequest;
import org.example.ecommerce.orders.dto.request.OrderScrollRequest;
import org.example.ecommerce.orders.dto.response.OrderPageResponse;
import org.example.ecommerce.orders.dto.response.OrderResponse;
import org.example.ecommerce.orders.service.OrderService;
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
@PreAuthorize("hasRole('USER')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                UriComponentsBuilder uriBuilder) {
        OrderResponse response = orderService.create(userId);
        return ResponseEntity.created(
            uriBuilder
                .path("/api/v1/orders/{orderId}")
                .buildAndExpand(response.id())
                .toUri()
        ).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> get(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                             @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.get(userId, orderId));
    }

    @GetMapping
    public ResponseEntity<OrderPageResponse> getMy(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                   @Valid @ModelAttribute OrderScrollRequest request) {
        return ResponseEntity.ok(orderService.getMy(userId, request));
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addItem(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                 @PathVariable Long orderId,
                                                 @Valid @RequestBody OrderAddItemRequest request) {
        return ResponseEntity.ok(orderService.addItem(userId, orderId, request));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderResponse> removeItem(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                    @PathVariable Long orderId,
                                                    @PathVariable Long itemId) {
        return ResponseEntity.ok(orderService.removeItem(userId, orderId, itemId));
    }

    @PatchMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> changeQuantity(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                        @PathVariable Long orderId,
                                                        @Valid @RequestBody OrderChangeQuantityRequest request) {
        return ResponseEntity.ok(orderService.changeQuantity(userId, orderId, request));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<OrderResponse> pay(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                             @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.pay(userId, orderId));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<OrderResponse> complete(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                  @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.complete(userId, orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancel(userId, orderId));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                       @PathVariable Long orderId) {
        orderService.delete(userId, orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/restore")
    public ResponseEntity<OrderResponse> restore(@AuthenticationPrincipal(expression = USER_CLAIM_EXPRESSION) Long userId,
                                                 @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.restore(userId, orderId));
    }

}
