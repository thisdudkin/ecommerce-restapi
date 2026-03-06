package org.example.ecommerce.users.controller;

import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.request.UserRequest;
import org.example.ecommerce.users.dto.request.UserUpdateRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.dto.response.UserListResponse;
import org.example.ecommerce.users.dto.response.UserResponse;
import org.example.ecommerce.users.dto.response.UserScrollResponse;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.example.ecommerce.users.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.example.ecommerce.users.utils.TestDataGenerator.id;
import static org.example.ecommerce.users.utils.TestDataGenerator.name;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCardRequest;
import static org.example.ecommerce.users.utils.TestDataGenerator.paymentCardResponse;
import static org.example.ecommerce.users.utils.TestDataGenerator.surname;
import static org.example.ecommerce.users.utils.TestDataGenerator.userListResponse;
import static org.example.ecommerce.users.utils.TestDataGenerator.userRequest;
import static org.example.ecommerce.users.utils.TestDataGenerator.userResponse;
import static org.example.ecommerce.users.utils.TestDataGenerator.userUpdateRequest;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/v1/users/{id} -> 200 OK")
    void getByIdShouldReturnUser() throws Exception {
        // Arrange
        Long userId = id();
        PaymentCardResponse card = paymentCardResponse();
        UserResponse response = userResponse(Set.of(card));

        when(userService.getById(userId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{id}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(response.id()))
            .andExpect(jsonPath("$.name").value(response.name()))
            .andExpect(jsonPath("$.surname").value(response.surname()))
            .andExpect(jsonPath("$.email").value(response.email()))
            .andExpect(jsonPath("$.active").value(response.active()));

        verify(userService).getById(userId);
    }

    @Test
    @DisplayName("GET /api/v1/users -> 200 OK")
    void getAllShouldReturnScrollResponse() throws Exception {
        // Arrange
        String name = name();
        String surname = surname();
        int size = 10;
        String cursor = "cursor-1";

        UserListResponse item = userListResponse();
        UserScrollResponse response = new UserScrollResponse(List.of(item), true, "next-cursor");

        when(userService.getAll(name, surname, size, SortDirection.ASC, cursor))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                .param("name", name)
                .param("surname", surname)
                .param("size", String.valueOf(size))
                .param("direction", "ASC")
                .param("cursor", cursor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(item.id()))
            .andExpect(jsonPath("$.items[0].name").value(item.name()))
            .andExpect(jsonPath("$.items[0].surname").value(item.surname()))
            .andExpect(jsonPath("$.items[0].email").value(item.email()))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.nextCursor").value("next-cursor"));

        verify(userService).getAll(name, surname, size, SortDirection.ASC, cursor);
    }

    @Test
    @DisplayName("GET /api/v1/users with default params -> 200 OK")
    void getAllShouldUseDefaultParams() throws Exception {
        // Arrange
        UserScrollResponse response = new UserScrollResponse(List.of(), false, null);

        when(userService.getAll(null, null, 20, SortDirection.DESC, null))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.nextCursor").isEmpty());

        verify(userService).getAll(null, null, 20, SortDirection.DESC, null);
    }

    @Test
    @DisplayName("GET /api/v1/users with invalid size -> 400 Bad Request")
    void getAllShouldReturnBadRequestWhenSizeIsInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users").param("size", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/users -> 201 Created")
    void createShouldReturnCreatedUser() throws Exception {
        // Arrange
        UserRequest request = userRequest(List.of(paymentCardRequest()));
        UserResponse response = userResponse();

        when(userService.create(request)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/v1/users/" + response.id()))
            .andExpect(jsonPath("$.id").value(response.id()))
            .andExpect(jsonPath("$.name").value(response.name()))
            .andExpect(jsonPath("$.surname").value(response.surname()))
            .andExpect(jsonPath("$.email").value(response.email()))
            .andExpect(jsonPath("$.active").value(response.active()));

        verify(userService).create(eq(request));
    }

    @Test
    @DisplayName("POST /api/v1/users with invalid body -> 400 Bad Request")
    void createShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        // Arrange
        UserRequest request = new UserRequest(
            "",
            "",
            LocalDate.now().plusDays(1),
            "not-email",
            List.of(
                new PaymentCardRequest("", "", LocalDate.now().minusDays(1))
            )
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} -> 200 OK")
    void updateShouldReturnUpdatedUser() throws Exception {
        // Arrange
        Long userId = id();
        UserUpdateRequest request = userUpdateRequest();
        UserResponse response = userResponse();

        when(userService.update(userId, request)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(response.id()))
            .andExpect(jsonPath("$.name").value(response.name()))
            .andExpect(jsonPath("$.surname").value(response.surname()))
            .andExpect(jsonPath("$.email").value(response.email()))
            .andExpect(jsonPath("$.active").value(response.active()));

        verify(userService).update(userId, request);
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} with invalid body -> 400 Bad Request")
    void updateShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        // Arrange
        Long userId = id();
        UserUpdateRequest request = new UserUpdateRequest(
            "",
            "",
            LocalDate.now().plusDays(1),
            "bad-email"
        );

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/activate -> 204 No Content")
    void activateShouldReturnNoContent() throws Exception {
        // Arrange
        Long userId = id();
        doNothing().when(userService).activate(userId);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/{id}/activate", userId))
            .andExpect(status().isNoContent());

        verify(userService).activate(userId);
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/deactivate -> 204 No Content")
    void deactivateShouldReturnNoContent() throws Exception {
        // Arrange
        Long userId = id();
        doNothing().when(userService).deactivate(userId);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/{id}/deactivate", userId))
            .andExpect(status().isNoContent());

        verify(userService).deactivate(userId);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} -> 204 No Content")
    void deleteShouldReturnNoContent() throws Exception {
        // Arrange
        Long userId = id();
        doNothing().when(userService).delete(userId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{id}", userId))
            .andExpect(status().isNoContent());

        verify(userService).delete(userId);
    }

}
