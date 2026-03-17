package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.dto.request.CursorPayload;
import org.example.ecommerce.orders.exception.custom.pagination.InvalidCursorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorServiceTests {

    private CursorService cursorService;

    @BeforeEach
    void setup() {
        cursorService = new CursorService(
            new ObjectMapper(),
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
    }

    @Test
    void encodeThenDecodeReturnsSamePayload() {
        CursorPayload source = new CursorPayload(
            LocalDateTime.of(2026, 1, 1, 10, 0),
            200L
        );

        String token = cursorService.encode(source);
        CursorPayload decoded = cursorService.decode(token);

        assertThat(decoded.createdAt()).isEqualTo(source.createdAt());
        assertThat(decoded.id()).isEqualTo(source.id());
    }

    @Test
    void decodeBlankTokenReturnsEmptyPayload() {
        CursorPayload decoded = cursorService.decode("  ");

        assertThat(decoded.createdAt()).isNull();
        assertThat(decoded.id()).isNull();
    }

    @Test
    void decodeInvalidTokenThrowsInvalidCursorException() {
        assertThatThrownBy(() -> cursorService.decode("broken-token"))
            .isInstanceOf(InvalidCursorException.class)
            .hasMessageContaining("Invalid cursor token");
    }

}
