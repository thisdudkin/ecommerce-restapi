package org.example.ecommerce.orders.controller;

import org.example.ecommerce.orders.dto.request.OrderAddItemRequest;
import org.example.ecommerce.orders.dto.request.OrderChangeQuantityRequest;
import org.example.ecommerce.orders.dto.request.OrderScrollRequest;
import org.example.ecommerce.orders.dto.response.OrderPageResponse;
import org.example.ecommerce.orders.dto.response.OrderResponse;
import org.example.ecommerce.orders.dto.response.UserResponse;
import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.exception.handler.RestExceptionHandler;
import org.example.ecommerce.orders.service.OrderService;
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

import static org.example.ecommerce.orders.enums.OrderStatus.NEW;
import static org.example.ecommerce.orders.support.TestDataGenerator.item;
import static org.example.ecommerce.orders.support.TestDataGenerator.order;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderAddItemRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderChangeQuantityRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderPageResponse;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderResponse;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderScrollRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.userResponse;
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

@WebMvcTest(OrderController.class)
@Import(MethodSecurityTestConfig.class)
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @SpringBootApplication
    @Import(RestExceptionHandler.class)
    static class TestApplication {
    }

    @Test
    void createWhenUserRoleReturnsCreatedAndPassesUserId() throws Exception {
        long userId = 1L;
        UserResponse user = userResponse(userId);
        Order createdOrder = order(200L, userId);
        OrderResponse response = orderResponse(createdOrder, user);

        when(orderService.create(userId))
            .thenReturn(response);

        mockMvc.perform(
                post("/api/v1/orders")
                    .with(authentication(userAuthentication()))
            )
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/v1/orders/200"))
            .andExpect(jsonPath("$.id").value(200))
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.user.id").value(1));

        verify(orderService).create(userId);
    }

    @Test
    void createWhenAdminRoleForbidden() throws Exception {
        mockMvc.perform(
                post("/api/v1/orders")
                    .with(authentication(adminAuthentication()))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void getWhenUserRoleReturnsOk() throws Exception {
        long userId = 1L;
        UserResponse user = userResponse(userId);
        Order existingOrder = order(200L, userId);
        Item orderItem = item(100L, "Keyboard", BigDecimal.valueOf(10.00));
        existingOrder.addItem(orderItem, 1);

        OrderResponse response = orderResponse(existingOrder, user);

        when(orderService.get(userId, 200L))
            .thenReturn(response);

        mockMvc.perform(
                get("/api/v1/orders/200")
                    .with(authentication(userAuthentication()))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(200))
            .andExpect(jsonPath("$.totalPrice").value(10.00))
            .andExpect(jsonPath("$.status").value("NEW"));

        verify(orderService).get(userId, 200L);
    }

    @Test
    void getMyWhenUserRoleReturnsOk() throws Exception {
        long userId = 1L;
        UserResponse user = userResponse(userId);

        OrderScrollRequest request = orderScrollRequest(2, NEW, null, null, null);

        Order firstOrder = order(200L, userId);
        firstOrder.addItem(item(100L, "Keyboard", BigDecimal.valueOf(10.00)), 1);

        OrderPageResponse response = orderPageResponse(
            List.of(orderResponse(firstOrder, user)),
            "next-token"
        );

        when(orderService.getMy(eq(userId), eq(request)))
            .thenReturn(response);

        mockMvc.perform(
                get("/api/v1/orders")
                    .with(authentication(userAuthentication()))
                    .param("size", "2")
                    .param("status", "NEW")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orders[0].id").value(200))
            .andExpect(jsonPath("$.orders[0].status").value("NEW"))
            .andExpect(jsonPath("$.token").value("next-token"));

        verify(orderService).getMy(userId, request);
    }

    @Test
    void addItemWhenUserRoleReturnsOk() throws Exception {
        long userId = 1L;
        OrderAddItemRequest request = orderAddItemRequest(100L, 2);

        UserResponse user = userResponse(userId);
        Order order = order(200L, userId);
        order.addItem(item(100L, "Keyboard", BigDecimal.valueOf(10.00)), 2);

        OrderResponse response = orderResponse(order, user);

        when(orderService.addItem(eq(userId), eq(200L), eq(request)))
            .thenReturn(response);

        mockMvc.perform(
                post("/api/v1/orders/200/items")
                    .with(authentication(userAuthentication()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(200))
            .andExpect(jsonPath("$.totalPrice").value(20.00))
            .andExpect(jsonPath("$.orderItems.length()").value(1));

        verify(orderService).addItem(userId, 200L, request);
    }

    @Test
    void changeQuantityWhenUserRoleReturnsOk() throws Exception {
        long userId = 1L;
        OrderChangeQuantityRequest request = orderChangeQuantityRequest(100L, 5);

        UserResponse user = userResponse(userId);
        Order order = order(200L, userId);
        order.addItem(item(100L, "Keyboard", BigDecimal.valueOf(10.00)), 1);
        order.changeItemQuantity(item(100L, "Keyboard", BigDecimal.valueOf(10.00)), 5);

        OrderResponse response = orderResponse(order, user);

        when(orderService.changeQuantity(eq(userId), eq(200L), eq(request)))
            .thenReturn(response);

        mockMvc.perform(
                patch("/api/v1/orders/200/items")
                    .with(authentication(userAuthentication()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(200))
            .andExpect(jsonPath("$.totalPrice").value(50.00));

        verify(orderService).changeQuantity(userId, 200L, request);
    }

    @Test
    void payWhenUserRoleReturnsOk() throws Exception {
        long userId = 1L;
        UserResponse user = userResponse(userId);

        Order order = order(200L, userId);
        order.addItem(item(100L, "Keyboard", BigDecimal.valueOf(10.00)), 2);
        order.markPaid();

        OrderResponse response = orderResponse(order, user);

        when(orderService.pay(userId, 200L))
            .thenReturn(response);

        mockMvc.perform(
                post("/api/v1/orders/200/pay")
                    .with(authentication(userAuthentication()))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.totalPrice").value(20.00));

        verify(orderService).pay(userId, 200L);
    }

    @Test
    void deleteWhenUserRoleReturnsNoContent() throws Exception {
        long userId = 1L;

        mockMvc.perform(
                delete("/api/v1/orders/200")
                    .with(authentication(userAuthentication()))
            )
            .andExpect(status().isNoContent());

        verify(orderService).delete(userId, 200L);
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
