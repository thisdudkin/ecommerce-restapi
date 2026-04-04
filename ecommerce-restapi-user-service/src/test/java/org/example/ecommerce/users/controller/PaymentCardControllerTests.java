package org.example.ecommerce.users.controller;

import org.example.ecommerce.users.controller.config.TestSecurityConfig;
import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.exception.custom.PaymentCardNotFoundException;
import org.example.ecommerce.users.exception.custom.PaymentCardNumberAlreadyExistsException;
import org.example.ecommerce.users.security.GatewayHeadersAuthenticationFilter;
import org.example.ecommerce.users.security.SecurityConfig;
import org.example.ecommerce.users.security.AccessGuard;
import org.example.ecommerce.users.service.PaymentCardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.example.ecommerce.users.utils.TestDataGenerator.id;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCardRequest;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCardResponse;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = PaymentCardController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GatewayHeadersAuthenticationFilter.class)
    }
)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentCardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentCardService cardService;

    @MockitoBean(name = "accessGuard")
    private AccessGuard accessGuard;

    @Test
    @DisplayName("GET /api/v1/users/{userId}/cards -> 200 OK")
    @WithMockUser(roles = "ADMIN")
    void getByUserIdShouldReturnCards() throws Exception {
        Long userId = id();
        PaymentCardResponse card = paymentCardResponse();

        when(cardService.getAllByUserId(userId)).thenReturn(List.of(card));

        mockMvc.perform(get("/api/v1/users/{userId}/cards", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(card.id()))
            .andExpect(jsonPath("$[0].number").value(card.number()))
            .andExpect(jsonPath("$[0].holder").value(card.holder()))
            .andExpect(jsonPath("$[0].active").value(card.active()));

        verify(cardService).getAllByUserId(userId);
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/cards -> 200 OK for owner")
    @WithMockUser(roles = "USER")
    void getByUserIdShouldReturnCardsWhenAccessGuardAllowsAccess() throws Exception {
        Long userId = id();
        PaymentCardResponse card = paymentCardResponse();

        when(accessGuard.canAccessUser(userId)).thenReturn(true);
        when(cardService.getAllByUserId(userId)).thenReturn(List.of(card));

        mockMvc.perform(get("/api/v1/users/{userId}/cards", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(card.id()));

        verify(accessGuard).canAccessUser(userId);
        verify(cardService).getAllByUserId(userId);
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/cards -> 403 Forbidden")
    @WithMockUser(roles = "USER")
    void getByUserIdShouldReturnForbiddenWhenAccessIsDenied() throws Exception {
        Long userId = id();

        when(accessGuard.canAccessUser(userId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/{userId}/cards", userId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Access denied"))
            .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

        verify(accessGuard).canAccessUser(userId);
        verifyNoInteractions(cardService);
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/cards/{cardId} -> 200 OK")
    @WithMockUser(roles = "ADMIN")
    void getByIdShouldReturnCard() throws Exception {
        Long userId = id();
        Long cardId = id();
        PaymentCardResponse card = paymentCardResponse();

        when(cardService.getById(userId, cardId)).thenReturn(card);

        mockMvc.perform(get("/api/v1/users/{userId}/cards/{cardId}", userId, cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(card.id()))
            .andExpect(jsonPath("$.number").value(card.number()))
            .andExpect(jsonPath("$.holder").value(card.holder()))
            .andExpect(jsonPath("$.active").value(card.active()));

        verify(cardService).getById(userId, cardId);
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/cards/{cardId} -> 404 Not Found")
    @WithMockUser(roles = "ADMIN")
    void getByIdShouldReturnNotFoundWhenCardDoesNotExist() throws Exception {
        Long userId = id();
        Long cardId = id();

        when(cardService.getById(userId, cardId))
            .thenThrow(new PaymentCardNotFoundException(cardId));

        mockMvc.perform(get("/api/v1/users/{userId}/cards/{cardId}", userId, cardId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Payment card not found"))
            .andExpect(jsonPath("$.detail").value("Payment card not found with id: " + cardId));
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/cards -> 201 Created")
    @WithMockUser(roles = "ADMIN")
    void createShouldReturnCreatedCard() throws Exception {
        Long userId = id();
        PaymentCardRequest request = paymentCardRequest();
        PaymentCardResponse response = paymentCardResponse();

        when(cardService.create(userId, request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string(
                "Location",
                "http://localhost/api/v1/users/" + userId + "/cards/" + response.id()
            ))
            .andExpect(jsonPath("$.id").value(response.id()))
            .andExpect(jsonPath("$.number").value(response.number()))
            .andExpect(jsonPath("$.holder").value(response.holder()))
            .andExpect(jsonPath("$.active").value(response.active()));

        verify(cardService).create(userId, request);
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/cards with invalid body -> 400 Bad Request")
    @WithMockUser(roles = "ADMIN")
    void createShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        Long userId = id();
        PaymentCardRequest request = new PaymentCardRequest(
            "",
            "",
            LocalDate.now().minusDays(1)
        );

        mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/cards when card number already exists -> 409 Conflict")
    @WithMockUser(roles = "ADMIN")
    void createShouldReturnConflictWhenCardNumberAlreadyExists() throws Exception {
        Long userId = id();
        PaymentCardRequest request = paymentCardRequest();

        when(cardService.create(userId, request))
            .thenThrow(new PaymentCardNumberAlreadyExistsException(
                List.of("1111222233334444")
            ));

        mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Payment card number already exists"))
            .andExpect(jsonPath("$.detail").value("Payment card number(s) already exist: 1111222233334444"))
            .andExpect(jsonPath("$.numbers[0]").value("1111222233334444"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{userId}/cards/{cardId} -> 200 OK")
    @WithMockUser(roles = "ADMIN")
    void updateShouldReturnUpdatedCard() throws Exception {
        Long userId = id();
        Long cardId = id();
        PaymentCardRequest request = paymentCardRequest();
        PaymentCardResponse response = paymentCardResponse();

        when(cardService.update(userId, cardId, request)).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/{userId}/cards/{cardId}", userId, cardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(response.id()))
            .andExpect(jsonPath("$.number").value(response.number()))
            .andExpect(jsonPath("$.holder").value(response.holder()))
            .andExpect(jsonPath("$.active").value(response.active()));

        verify(cardService).update(userId, cardId, request);
    }

    @Test
    @DisplayName("PUT /api/v1/users/{userId}/cards/{cardId} with invalid body -> 400 Bad Request")
    @WithMockUser(roles = "ADMIN")
    void updateShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        Long userId = id();
        Long cardId = id();
        PaymentCardRequest request = new PaymentCardRequest(
            "",
            "",
            LocalDate.now().minusDays(10)
        );

        mockMvc.perform(put("/api/v1/users/{userId}/cards/{cardId}", userId, cardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{userId}/cards/{cardId}/activate -> 204 No Content")
    @WithMockUser(roles = "ADMIN")
    void activateShouldReturnNoContent() throws Exception {
        Long userId = id();
        Long cardId = id();
        doNothing().when(cardService).activate(userId, cardId);

        mockMvc.perform(patch("/api/v1/users/{userId}/cards/{cardId}/activate", userId, cardId))
            .andExpect(status().isNoContent());

        verify(cardService).activate(userId, cardId);
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{userId}/cards/{cardId}/deactivate -> 204 No Content")
    @WithMockUser(roles = "ADMIN")
    void deactivateShouldReturnNoContent() throws Exception {
        Long userId = id();
        Long cardId = id();
        doNothing().when(cardService).deactivate(userId, cardId);

        mockMvc.perform(patch("/api/v1/users/{userId}/cards/{cardId}/deactivate", userId, cardId))
            .andExpect(status().isNoContent());

        verify(cardService).deactivate(userId, cardId);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{userId}/cards/{cardId} -> 204 No Content")
    @WithMockUser(roles = "ADMIN")
    void deleteShouldReturnNoContent() throws Exception {
        Long userId = id();
        Long cardId = id();
        doNothing().when(cardService).delete(userId, cardId);

        mockMvc.perform(delete("/api/v1/users/{userId}/cards/{cardId}", userId, cardId))
            .andExpect(status().isNoContent());

        verify(cardService).delete(userId, cardId);
    }

}
