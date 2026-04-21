package org.example.ecommerce.users.utils;

import org.example.ecommerce.users.dto.request.UserCursorPayload;
import org.example.ecommerce.users.exception.custom.InvalidCursorException;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserCursorCodec {

    private final ObjectMapper objectMapper;
    private final Base64.Encoder base64Encoder;
    private final Base64.Decoder base64Decoder;

    public UserCursorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.base64Encoder = Base64.getUrlEncoder().withoutPadding();
        this.base64Decoder = Base64.getUrlDecoder();
    }

    public UserCursorPayload decode(String cursor, SortDirection requestedDirection) {
        try {
            String json = new String(base64Decoder.decode(cursor), StandardCharsets.UTF_8);
            UserCursorPayload payload = objectMapper.readValue(json, UserCursorPayload.class);

            if (payload.direction() != requestedDirection)
                throw new InvalidCursorException("Cursor direction does not match request direction");

            return payload;
        } catch (InvalidCursorException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCursorException("Cursor is invalid or malformed");
        }
    }

    public String encode(LocalDateTime createdAt, Long id, SortDirection direction) {
        try {
            UserCursorPayload payload = new UserCursorPayload(createdAt, id, direction);
            String json = objectMapper.writeValueAsString(payload);
            return base64Encoder.encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new InvalidCursorException("Failed to encode cursor");
        }
    }

}
