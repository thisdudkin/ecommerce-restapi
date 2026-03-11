package org.example.ecommerce.users.utils;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.example.ecommerce.users.dto.request.UserCursorPayload;
import org.example.ecommerce.users.exception.custom.InvalidCursorException;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserCursorCodecTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldEncodeCursorToUrlSafeBase64String() {
        // Arrange
        UserCursorCodec codec = new UserCursorCodec(objectMapper);

        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 10, 12, 30, 45);
        Long id = 123L;

        // Act
        String cursor = codec.encode(createdAt, id, SortDirection.ASC);

        // Assert
        assertThat(cursor)
            .isNotBlank()
            .doesNotContain("+")
            .doesNotContain("/")
            .doesNotContain("=");
    }

    @Test
    void shouldDecodeValidCursorToForwardScrollPosition() {
        // Arrange
        UserCursorCodec codec = new UserCursorCodec(objectMapper);

        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 10, 12, 30, 45);
        Long id = 123L;

        UserCursorPayload payload = new UserCursorPayload(createdAt, id, SortDirection.ASC);
        String json = objectMapper.writeValueAsString(payload);
        String cursor = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.getBytes(StandardCharsets.UTF_8));

        // Act
        ScrollPosition position = codec.decode(cursor, SortDirection.ASC);

        // Assert
        assertThat(position).isInstanceOf(KeysetScrollPosition.class);

        KeysetScrollPosition keysetPosition = (KeysetScrollPosition) position;
        Map<String, Object> keys = keysetPosition.getKeys();

        assertThat(keys)
            .containsEntry("createdAt", createdAt)
            .containsEntry("id", id);
    }

    @Test
    void shouldPreserveCreatedAtAndIdWhenEncodingAndDecodingCursor() {
        // Arrange
        UserCursorCodec codec = new UserCursorCodec(objectMapper);

        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 15, 8, 45, 0);
        Long id = 999L;

        // Act
        String cursor = codec.encode(createdAt, id, SortDirection.DESC);
        ScrollPosition position = codec.decode(cursor, SortDirection.DESC);

        // Assert
        assertThat(position).isInstanceOf(KeysetScrollPosition.class);

        KeysetScrollPosition keysetPosition = (KeysetScrollPosition) position;
        Map<String, Object> keys = keysetPosition.getKeys();

        assertThat(keys)
            .containsEntry("createdAt", createdAt)
            .containsEntry("id", id);
    }

    @Test
    void shouldThrowInvalidCursorExceptionWhenRequestedDirectionDoesNotMatchCursorDirection() {
        // Arrange
        UserCursorCodec codec = new UserCursorCodec(objectMapper);
        LocalDateTime createdAt = LocalDateTime.of(2024, 6, 1, 10, 0, 0);

        // Act
        String cursor = codec.encode(createdAt, 55L, SortDirection.ASC);

        // Assert
        assertThatThrownBy(() -> codec.decode(cursor, SortDirection.DESC))
            .isInstanceOf(InvalidCursorException.class)
            .hasMessage("Cursor direction does not match request direction");
    }

    @Test
    void shouldThrowInvalidCursorExceptionWhenCursorIsNotValidBase64() {
        // Arrange
        UserCursorCodec codec = new UserCursorCodec(objectMapper);

        // Act & Assert
        assertThatThrownBy(() -> codec.decode("%%%not-valid-base64%%%", SortDirection.ASC))
            .isInstanceOf(InvalidCursorException.class)
            .hasMessage("Cursor is invalid or malformed");
    }

    @Test
    void shouldThrowInvalidCursorExceptionWhenCursorContainsInvalidJson() {
        // Arrange
        UserCursorCodec codec = new UserCursorCodec(objectMapper);

        String cursor = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("not-json".getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThatThrownBy(() -> codec.decode(cursor, SortDirection.ASC))
            .isInstanceOf(InvalidCursorException.class)
            .hasMessage("Cursor is invalid or malformed");
    }

    @Test
    void shouldThrowInvalidCursorExceptionWhenCursorPayloadDoesNotContainDirection() {
        // Arrange
        UserCursorCodec codec = new UserCursorCodec(objectMapper);

        String json = "{\"id\":123}";
        String cursor = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThatThrownBy(() -> codec.decode(cursor, SortDirection.ASC))
            .isInstanceOf(InvalidCursorException.class)
            .hasMessage("Cursor direction does not match request direction");
    }

    @Test
    void shouldThrowInvalidCursorExceptionWhenObjectMapperFailsToReadPayload() {
        // Arrange
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);

        when(failingObjectMapper.readValue(anyString(), eq(UserCursorPayload.class)))
            .thenThrow(new RuntimeException("boom"));

        UserCursorCodec codec = new UserCursorCodec(failingObjectMapper);

        String cursor = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"some\":\"json\"}".getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThatThrownBy(() -> codec.decode(cursor, SortDirection.ASC))
            .isInstanceOf(InvalidCursorException.class)
            .hasMessage("Cursor is invalid or malformed");
    }

    @Test
    void shouldThrowInvalidCursorExceptionWhenObjectMapperFailsToEncodePayload() {
        // Arrange
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        UserCursorCodec codec = new UserCursorCodec(failingObjectMapper);

        when(failingObjectMapper.writeValueAsString(any()))
            .thenThrow(new RuntimeException("boom"));

        ThrowingCallable act = () -> codec.encode(LocalDateTime.now(), 1L, SortDirection.ASC);

        // Act & Assert
        assertThatThrownBy(act)
            .isInstanceOf(InvalidCursorException.class)
            .hasMessage("Failed to encode cursor");
    }

}
