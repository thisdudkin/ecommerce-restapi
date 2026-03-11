package org.example.ecommerce.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.example.ecommerce.users.dto.request.UserRequest;
import org.example.ecommerce.users.dto.request.UserUpdateRequest;
import org.example.ecommerce.users.dto.response.UserResponse;
import org.example.ecommerce.users.dto.response.UserScrollResponse;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.example.ecommerce.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    public ResponseEntity<UserScrollResponse> getAll(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String surname,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
        @RequestParam(defaultValue = "DESC") String direction,
        @RequestParam(required = false) String cursor
    ) {
        SortDirection sort = SortDirection.valueOf(direction.toUpperCase());
        return ResponseEntity.ok(userService.getAll(name, surname, size, sort, cursor));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request,
                                               UriComponentsBuilder uriBuilder
    ) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(
            uriBuilder
                .path("/api/v1/users/{id}")
                .buildAndExpand(created.id())
                .toUri()
        ).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        userService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
