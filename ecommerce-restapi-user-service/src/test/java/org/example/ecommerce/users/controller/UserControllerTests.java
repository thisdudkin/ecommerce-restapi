package org.example.ecommerce.users.controller;

import org.example.ecommerce.users.controller.config.TestSecurityConfig;
import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.request.UserRequest;
import org.example.ecommerce.users.dto.request.UserUpdateRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.dto.response.UserListResponse;
import org.example.ecommerce.users.dto.response.UserResponse;
import org.example.ecommerce.users.dto.response.UserScrollResponse;
import org.example.ecommerce.users.exception.custom.UserEmailAlreadyExistsException;
import org.example.ecommerce.users.exception.custom.UserNotFoundException;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.example.ecommerce.users.security.config.SecurityConfig;
import org.example.ecommerce.users.security.filter.JwtAuthenticationFilter;
import org.example.ecommerce.users.security.jwt.AccessGuard;
import org.example.ecommerce.users.service.UserService;
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
    value = UserController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
    }
)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean(name = "accessGuard")
    private AccessGuard accessGuard;

    @Test
    @DisplayName("GET /api/v1/users/{id} -> 200 OK")
    @WithMockUser(roles = "ADMIN")
    void getByIdShouldReturnUser() throws Exception {
        Long userId = id();
        PaymentCardResponse card = paymentCardResponse();
        UserResponse response = userResponse(Set.of(card));

        when(userService.getById(userId)).thenReturn(response);

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
    @DisplayName("GET /api/v1/users/{id} -> 200 OK for owner")
    @WithMockUser(roles = "USER")
    void getByIdShouldReturnUserWhenAccessGuardAllowsAccess() throws Exception {
        Long userId = id();
        UserResponse response = userResponse();

        when(accessGuard.canAccessUser(userId)).thenReturn(true);
        when(userService.getById(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(response.id()));

        verify(accessGuard).canAccessUser(userId);
        verify(userService).getById(userId);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} -> 403 Forbidden")
    @WithMockUser(roles = "USER")
    void getByIdShouldReturnForbiddenWhenAccessIsDenied() throws Exception {
        Long userId = id();

        when(accessGuard.canAccessUser(userId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Access denied"))
            .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

        verify(accessGuard).canAccessUser(userId);
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} -> 404 Not Found")
    @WithMockUser(roles = "ADMIN")
    void getByIdShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        Long userId = id();
        when(userService.getById(userId))
            .thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(get("/api/v1/users/{id}", userId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("User not found"))
            .andExpect(jsonPath("$.detail").value("User not found with id: " + userId));
    }

    @Test
    @DisplayName("GET /api/v1/users -> 200 OK")
    @WithMockUser(roles = "ADMIN")
    void getAllShouldReturnScrollResponse() throws Exception {
        String name = name();
        String surname = surname();
        int size = 10;
        String cursor = "cursor-1";

        UserListResponse item = userListResponse();
        UserScrollResponse response = new UserScrollResponse(List.of(item), true, "next-cursor");

        when(userService.getAll(name, surname, size, SortDirection.ASC, cursor))
            .thenReturn(response);

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
    @WithMockUser(roles = "ADMIN")
    void getAllShouldUseDefaultParams() throws Exception {
        UserScrollResponse response = new UserScrollResponse(List.of(), false, null);

        when(userService.getAll(null, null, 20, SortDirection.DESC, null))
            .thenReturn(response);

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.nextCursor").isEmpty());

        verify(userService).getAll(null, null, 20, SortDirection.DESC, null);
    }

    @Test
    @DisplayName("GET /api/v1/users with invalid size -> 400 Bad Request")
    @WithMockUser(roles = "ADMIN")
    void getAllShouldReturnBadRequestWhenSizeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("size", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/users -> 403 Forbidden for regular user")
    @WithMockUser(roles = "USER")
    void getAllShouldReturnForbiddenForRegularUser() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Access denied"))
            .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("POST /api/v1/users -> 201 Created")
    @WithMockUser(roles = "INTERNAL_SERVICE")
    void createShouldReturnCreatedUser() throws Exception {
        UserRequest request = userRequest(List.of(paymentCardRequest()));
        UserResponse response = userResponse();

        when(userService.create(request)).thenReturn(response);

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

        verify(userService).create(request);
    }

    @Test
    @DisplayName("POST /api/v1/users -> 403 Forbidden for regular user")
    @WithMockUser(roles = "USER")
    void createShouldReturnForbiddenWhenRoleIsInvalid() throws Exception {
        UserRequest request = userRequest(List.of(paymentCardRequest()));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Access denied"))
            .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("POST /api/v1/users with duplicate email -> 409 Conflict")
    @WithMockUser(roles = "INTERNAL_SERVICE")
    void createShouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        UserRequest request = userRequest(List.of(paymentCardRequest()));

        when(userService.create(request))
            .thenThrow(new UserEmailAlreadyExistsException(request.email()));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("User email already exists"))
            .andExpect(jsonPath("$.detail").value("User with email already exists: " + request.email()));
    }

    @Test
    @DisplayName("POST /api/v1/users with invalid body -> 400 Bad Request")
    @WithMockUser(roles = "INTERNAL_SERVICE")
    void createShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        UserRequest request = new UserRequest(
            "",
            "",
            LocalDate.now().plusDays(1),
            "not-email",
            List.of(
                new PaymentCardRequest("", "", LocalDate.now().minusDays(1))
            )
        );

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} -> 200 OK")
    @WithMockUser(roles = "ADMIN")
    void updateShouldReturnUpdatedUser() throws Exception {
        Long userId = id();
        UserUpdateRequest request = userUpdateRequest();
        UserResponse response = userResponse();

        when(userService.update(userId, request)).thenReturn(response);

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
    @WithMockUser(roles = "ADMIN")
    void updateShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        Long userId = id();
        UserUpdateRequest request = new UserUpdateRequest(
            "",
            "",
            LocalDate.now().plusDays(1),
            "bad-email"
        );

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/activate -> 204 No Content")
    @WithMockUser(roles = "ADMIN")
    void activateShouldReturnNoContent() throws Exception {
        Long userId = id();
        doNothing().when(userService).activate(userId);

        mockMvc.perform(patch("/api/v1/users/{id}/activate", userId))
            .andExpect(status().isNoContent());

        verify(userService).activate(userId);
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/deactivate -> 204 No Content")
    @WithMockUser(roles = "ADMIN")
    void deactivateShouldReturnNoContent() throws Exception {
        Long userId = id();
        doNothing().when(userService).deactivate(userId);

        mockMvc.perform(patch("/api/v1/users/{id}/deactivate", userId))
            .andExpect(status().isNoContent());

        verify(userService).deactivate(userId);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} -> 204 No Content")
    @WithMockUser(roles = "ADMIN")
    void deleteShouldReturnNoContent() throws Exception {
        Long userId = id();
        doNothing().when(userService).delete(userId);

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
            .andExpect(status().isNoContent());

        verify(userService).delete(userId);
    }

}
