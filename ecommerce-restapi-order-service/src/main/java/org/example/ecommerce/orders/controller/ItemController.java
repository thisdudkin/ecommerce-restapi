package org.example.ecommerce.orders.controller;

import jakarta.validation.Valid;
import org.example.ecommerce.orders.dto.request.ItemCreateRequest;
import org.example.ecommerce.orders.dto.request.ItemScrollRequest;
import org.example.ecommerce.orders.dto.request.ItemUpdateRequest;
import org.example.ecommerce.orders.dto.response.ItemPageResponse;
import org.example.ecommerce.orders.dto.response.ItemResponse;
import org.example.ecommerce.orders.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemCreateRequest request,
                                               UriComponentsBuilder uriBuilder) {
        ItemResponse response = itemService.create(request);
        return ResponseEntity.created(
            uriBuilder
                .path("/api/v1/items/{id}")
                .buildAndExpand(response.id())
                .toUri()
        ).body(response);
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ItemResponse> getById(@Valid @PathVariable Long itemId) {
        return ResponseEntity.ok(itemService.get(itemId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ItemPageResponse> getAll(@Valid @ModelAttribute ItemScrollRequest request) {
        return ResponseEntity.ok(itemService.getAll(request));
    }

    @PatchMapping("/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> update(@Valid @PathVariable Long itemId,
                                               @Valid @RequestBody ItemUpdateRequest request) {
        return ResponseEntity.ok(itemService.update(itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@Valid @PathVariable Long itemId) {
        itemService.delete(itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{itemId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> restore(@PathVariable Long itemId) {
        return ResponseEntity.ok(itemService.restore(itemId));
    }

}
