package org.example.ecommerce.users.controller;

import jakarta.validation.Valid;
import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.service.PaymentCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/users/{userId}/cards")
public class PaymentCardController {

    private final PaymentCardService cardService;

    public PaymentCardController(PaymentCardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.canAccessUser(#userId)")
    public ResponseEntity<List<PaymentCardResponse>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(cardService.getAllByUserId(userId));
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.canAccessUser(#userId)")
    public ResponseEntity<PaymentCardResponse> getById(@PathVariable Long userId,
                                                       @PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getById(userId, cardId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.canAccessUser(#userId)")
    public ResponseEntity<PaymentCardResponse> create(@PathVariable Long userId,
                                                      @Valid @RequestBody PaymentCardRequest request,
                                                      UriComponentsBuilder uriBuilder) {
        PaymentCardResponse created = cardService.create(userId, request);

        return ResponseEntity.created(
            uriBuilder.path("/api/v1/users/{userId}/cards/{cardId}")
                .buildAndExpand(userId, created.id())
                .toUri()
        ).body(created);
    }

    @PutMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.canAccessUser(#userId)")
    public ResponseEntity<PaymentCardResponse> update(@PathVariable Long userId,
                                                      @PathVariable Long cardId,
                                                      @Valid @RequestBody PaymentCardRequest request) {
        return ResponseEntity.ok(cardService.update(userId, cardId, request));
    }

    @PatchMapping("/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.canAccessUser(#userId)")
    public ResponseEntity<Void> activate(@PathVariable Long userId,
                                         @PathVariable Long cardId) {
        cardService.activate(userId, cardId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{cardId}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.canAccessUser(#userId)")
    public ResponseEntity<Void> deactivate(@PathVariable Long userId,
                                           @PathVariable Long cardId) {
        cardService.deactivate(userId, cardId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.canAccessUser(#userId)")
    public ResponseEntity<Void> delete(@PathVariable Long userId,
                                       @PathVariable Long cardId) {
        cardService.delete(userId, cardId);
        return ResponseEntity.noContent().build();
    }

}
