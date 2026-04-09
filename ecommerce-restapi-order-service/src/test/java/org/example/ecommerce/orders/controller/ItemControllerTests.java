package org.example.ecommerce.orders.controller;

import org.example.ecommerce.orders.dto.request.ItemCreateRequest;
import org.example.ecommerce.orders.dto.request.ItemScrollRequest;
import org.example.ecommerce.orders.dto.request.ItemUpdateRequest;
import org.example.ecommerce.orders.dto.response.ItemPageResponse;
import org.example.ecommerce.orders.dto.response.ItemResponse;
import org.example.ecommerce.orders.exception.handler.RestExceptionHandler;
import org.example.ecommerce.orders.service.ItemService;
import org.example.ecommerce.orders.support.MethodSecurityTestConfig;
import org.example.ecommerce.orders.support.TestPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.example.ecommerce.orders.support.TestDataGenerator.item;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemCreateRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemPageResponse;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemResponse;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemScrollRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemUpdateRequest;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@Import(MethodSecurityTestConfig.class)
class ItemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ItemService itemService;

    @SpringBootApplication
    @Import(RestExceptionHandler.class)
    static class TestApplication {
    }

    @Test
    void createWhenAdminRoleReturnsCreated() throws Exception {
        ItemCreateRequest request = itemCreateRequest();
        ItemResponse response = itemResponse(
            item(100L, request.name(), request.price())
        );

        when(itemService.create(eq(request)))
            .thenReturn(response);

        mockMvc.perform(
                post("/api/v1/items")
                    .with(authentication(adminAuthentication()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/v1/items/100"))
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.name").value(request.name()))
            .andExpect(jsonPath("$.price", comparesEqualTo(request.price()), BigDecimal.class));

        verify(itemService).create(request);
    }

    @Test
    void createWhenUserRoleForbidden() throws Exception {
        ItemCreateRequest request = itemCreateRequest();

        mockMvc.perform(
                post("/api/v1/items")
                    .with(authentication(userAuthentication()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void getByIdWhenUserRoleReturnsOk() throws Exception {
        ItemResponse response = itemResponse(item(100L));

        when(itemService.get(100L))
            .thenReturn(response);

        mockMvc.perform(
                get("/api/v1/items/100")
                    .with(authentication(userAuthentication()))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.name").value(response.name()))
            .andExpect(jsonPath("$.price", comparesEqualTo(response.price()), BigDecimal.class));

        verify(itemService).get(100L);
    }

    @Test
    void getAllWhenAdminRoleReturnsOk() throws Exception {
        ItemScrollRequest request = itemScrollRequest(10, null);
        ItemResponse response = itemResponse(item(100L));
        ItemPageResponse page = itemPageResponse(List.of(response), "next-token");

        when(itemService.getAll(eq(request)))
            .thenReturn(page);

        mockMvc.perform(
                get("/api/v1/items")
                    .with(authentication(adminAuthentication()))
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(100))
            .andExpect(jsonPath("$.items[0].name").value(response.name()))
            .andExpect(jsonPath("$.token").value("next-token"));

        verify(itemService).getAll(request);
    }

    @Test
    void updateWhenAdminRoleReturnsOk() throws Exception {
        ItemUpdateRequest request = itemUpdateRequest();
        ItemResponse response = itemResponse(
            item(100L, request.name(), request.price())
        );

        when(itemService.update(eq(100L), eq(request)))
            .thenReturn(response);

        mockMvc.perform(
                patch("/api/v1/items/100")
                    .with(authentication(adminAuthentication()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.name").value(request.name()))
            .andExpect(jsonPath("$.price", comparesEqualTo(request.price()), BigDecimal.class));

        verify(itemService).update(100L, request);
    }

    @Test
    void deleteWhenAdminRoleReturnsNoContent() throws Exception {
        mockMvc.perform(
                delete("/api/v1/items/100")
                    .with(authentication(adminAuthentication()))
            )
            .andExpect(status().isNoContent());

        verify(itemService).delete(100L);
    }

    @Test
    void restoreWhenUserRoleForbidden() throws Exception {
        mockMvc.perform(
                post("/api/v1/items/100/restore")
                    .with(authentication(userAuthentication()))
            )
            .andExpect(status().isForbidden());
    }

    private Authentication userAuthentication() {
        return new UsernamePasswordAuthenticationToken(
            new TestPrincipal(1L),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private Authentication adminAuthentication() {
        return new UsernamePasswordAuthenticationToken(
            new TestPrincipal(999L),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

}
