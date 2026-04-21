package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.client.UserClient;
import org.example.ecommerce.orders.dto.request.OrderAddItemRequest;
import org.example.ecommerce.orders.dto.request.OrderChangeQuantityRequest;
import org.example.ecommerce.orders.dto.request.OrderScrollRequest;
import org.example.ecommerce.orders.dto.response.OrderPageData;
import org.example.ecommerce.orders.dto.response.OrderPageResponse;
import org.example.ecommerce.orders.dto.response.OrderResponse;
import org.example.ecommerce.orders.dto.response.UserResponse;
import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.enums.OrderStatus;
import org.example.ecommerce.orders.exception.custom.feign.UserNotFoundException;
import org.example.ecommerce.orders.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.example.ecommerce.orders.enums.OrderStatus.PAID;
import static org.example.ecommerce.orders.support.TestDataGenerator.id;
import static org.example.ecommerce.orders.support.TestDataGenerator.order;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderAddItemRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderChangeQuantityRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderResponse;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderScrollRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.userResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderOrchestratorTests {

    @Mock
    private UserClient client;

    @Mock
    private OrderMapper mapper;

    @Mock
    private OrderService service;

    @InjectMocks
    private OrderOrchestrator orchestrator;

    @Test
    void createShouldLoadUserCallServiceAndMapResponse() {
        Long userId = id();

        UserResponse user = userResponse(userId);
        Order createdOrder = order(200L, userId);
        OrderResponse expected = orderResponse(createdOrder, user);

        when(client.getById(userId)).thenReturn(user);
        when(service.create(userId)).thenReturn(createdOrder);
        when(mapper.toResponse(createdOrder, user)).thenReturn(expected);

        OrderResponse actual = orchestrator.create(userId);

        assertSame(expected, actual);
        verify(client).getById(userId);
        verify(service).create(userId);
        verify(mapper).toResponse(createdOrder, user);
    }

    @Test
    void getShouldLoadUserCallServiceAndMapResponse() {
        Long userId = id();
        Long orderId = 200L;

        UserResponse user = userResponse(userId);
        Order existingOrder = order(orderId, userId);
        OrderResponse expected = orderResponse(existingOrder, user);

        when(client.getById(userId)).thenReturn(user);
        when(service.get(userId, orderId)).thenReturn(existingOrder);
        when(mapper.toResponse(existingOrder, user)).thenReturn(expected);

        OrderResponse actual = orchestrator.get(userId, orderId);

        assertSame(expected, actual);
        verify(client).getById(userId);
        verify(service).get(userId, orderId);
        verify(mapper).toResponse(existingOrder, user);
    }

    @Test
    void getMyShouldReturnEmptyPageWhenServiceReturnsNoOrders() {
        Long userId = 1L;
        OrderScrollRequest request = orderScrollRequest(2, null, null, null, null);

        when(service.getMy(userId, request))
            .thenReturn(new OrderPageData(List.of(), null));

        OrderPageResponse actual = orchestrator.getMy(userId, request);

        assertEquals(OrderPageResponse.empty(), actual);
        verify(service).getMy(userId, request);
        verify(client, never()).getByIds(any());
        verify(mapper, never()).toResponse(any(), any());
    }

    @Test
    void getMyShouldLoadDistinctUsersOnceAndMapResponses() {
        Long userId = 1L;

        Order firstOrder = order(101L, userId);
        Order secondOrder = order(102L, userId);

        UserResponse user = userResponse(userId);
        OrderResponse firstResponse = orderResponse(firstOrder, user);
        OrderResponse secondResponse = orderResponse(secondOrder, user);

        OrderScrollRequest request = orderScrollRequest(2, null, null, null, "cursor");
        OrderPageData page = new OrderPageData(List.of(firstOrder, secondOrder), "next-token");

        when(service.getMy(userId, request)).thenReturn(page);
        when(client.getByIds(Set.of(userId))).thenReturn(List.of(user));
        when(mapper.toResponse(firstOrder, user)).thenReturn(firstResponse);
        when(mapper.toResponse(secondOrder, user)).thenReturn(secondResponse);

        OrderPageResponse actual = orchestrator.getMy(userId, request);

        assertEquals(List.of(firstResponse, secondResponse), actual.orders());
        assertEquals("next-token", actual.token());

        verify(service).getMy(userId, request);
        verify(client).getByIds(Set.of(userId));
        verify(mapper).toResponse(firstOrder, user);
        verify(mapper).toResponse(secondOrder, user);
    }

    @Test
    void getMyShouldThrowWhenSomeUsersAreMissing() {
        Long firstUserId = 1L;
        Long secondUserId = 2L;

        Order firstOrder = order(101L, firstUserId);
        Order secondOrder = order(102L, secondUserId);

        OrderScrollRequest request = orderScrollRequest(2, null, null, null, "cursor");
        OrderPageData page = new OrderPageData(List.of(firstOrder, secondOrder), "next-token");

        when(service.getMy(firstUserId, request)).thenReturn(page);
        when(client.getByIds(Set.of(firstUserId, secondUserId)))
            .thenReturn(List.of(userResponse(firstUserId)));

        assertThrows(UserNotFoundException.class, () -> orchestrator.getMy(firstUserId, request));

        verify(service).getMy(firstUserId, request);
        verify(client).getByIds(Set.of(firstUserId, secondUserId));
        verify(mapper, never()).toResponse(any(), any());
    }

    @Test
    void getAllShouldReturnEmptyPageWhenServiceReturnsNoOrders() {
        OrderScrollRequest request = orderScrollRequest(2, null, null, null, null);

        when(service.getAll(request))
            .thenReturn(new OrderPageData(List.of(), null));

        OrderPageResponse actual = orchestrator.getAll(request);

        assertEquals(OrderPageResponse.empty(), actual);
        verify(service).getAll(request);
        verify(client, never()).getByIds(any());
        verify(mapper, never()).toResponse(any(), any());
    }

    @Test
    void getAllShouldLoadUsersOnceAndMapResponses() {
        Long firstUserId = 1L;
        Long secondUserId = 2L;

        Order firstOrder = order(101L, firstUserId);
        Order secondOrder = order(102L, secondUserId);

        UserResponse firstUser = userResponse(firstUserId);
        UserResponse secondUser = userResponse(secondUserId);

        OrderResponse firstResponse = orderResponse(firstOrder, firstUser);
        OrderResponse secondResponse = orderResponse(secondOrder, secondUser);

        OrderScrollRequest request = orderScrollRequest(2, null, null, null, "cursor");
        OrderPageData page = new OrderPageData(List.of(firstOrder, secondOrder), "next-token");

        when(service.getAll(request)).thenReturn(page);
        when(client.getByIds(Set.of(firstUserId, secondUserId)))
            .thenReturn(List.of(firstUser, secondUser));
        when(mapper.toResponse(firstOrder, firstUser)).thenReturn(firstResponse);
        when(mapper.toResponse(secondOrder, secondUser)).thenReturn(secondResponse);

        OrderPageResponse actual = orchestrator.getAll(request);

        assertEquals(List.of(firstResponse, secondResponse), actual.orders());
        assertEquals("next-token", actual.token());

        verify(service).getAll(request);
        verify(client).getByIds(Set.of(firstUserId, secondUserId));
        verify(mapper).toResponse(firstOrder, firstUser);
        verify(mapper).toResponse(secondOrder, secondUser);
    }

    @Test
    void getAllShouldThrowWhenSomeUsersAreMissing() {
        Long firstUserId = 1L;
        Long secondUserId = 2L;

        Order firstOrder = order(101L, firstUserId);
        Order secondOrder = order(102L, secondUserId);

        OrderScrollRequest request = orderScrollRequest(2, null, null, null, "cursor");
        OrderPageData page = new OrderPageData(List.of(firstOrder, secondOrder), "next-token");

        when(service.getAll(request)).thenReturn(page);
        when(client.getByIds(Set.of(firstUserId, secondUserId)))
            .thenReturn(List.of(userResponse(firstUserId)));

        assertThrows(UserNotFoundException.class, () -> orchestrator.getAll(request));

        verify(service).getAll(request);
        verify(client).getByIds(Set.of(firstUserId, secondUserId));
        verify(mapper, never()).toResponse(any(), any());
    }

    @Test
    void addItemShouldLoadUserCallServiceAndMapResponse() {
        Long userId = 1L;
        Long orderId = 200L;
        OrderAddItemRequest request = orderAddItemRequest(100L, 2);

        UserResponse user = userResponse(userId);
        Order updatedOrder = order(orderId, userId);
        OrderResponse expected = orderResponse(updatedOrder, user);

        when(client.getById(userId)).thenReturn(user);
        when(service.addItem(userId, orderId, request)).thenReturn(updatedOrder);
        when(mapper.toResponse(updatedOrder, user)).thenReturn(expected);

        OrderResponse actual = orchestrator.addItem(userId, orderId, request);

        assertSame(expected, actual);
        verify(client).getById(userId);
        verify(service).addItem(userId, orderId, request);
        verify(mapper).toResponse(updatedOrder, user);
    }

    @Test
    void removeItemShouldLoadUserCallServiceAndMapResponse() {
        Long userId = 1L;
        Long orderId = 200L;
        Long itemId = 100L;

        UserResponse user = userResponse(userId);
        Order updatedOrder = order(orderId, userId);
        OrderResponse expected = orderResponse(updatedOrder, user);

        when(client.getById(userId)).thenReturn(user);
        when(service.removeItem(userId, orderId, itemId)).thenReturn(updatedOrder);
        when(mapper.toResponse(updatedOrder, user)).thenReturn(expected);

        OrderResponse actual = orchestrator.removeItem(userId, orderId, itemId);

        assertSame(expected, actual);
        verify(client).getById(userId);
        verify(service).removeItem(userId, orderId, itemId);
        verify(mapper).toResponse(updatedOrder, user);
    }

    @Test
    void changeQuantityShouldLoadUserCallServiceAndMapResponse() {
        Long userId = 1L;
        Long orderId = 200L;
        OrderChangeQuantityRequest request = orderChangeQuantityRequest(100L, 5);

        UserResponse user = userResponse(userId);
        Order updatedOrder = order(orderId, userId);
        OrderResponse expected = orderResponse(updatedOrder, user);

        when(client.getById(userId)).thenReturn(user);
        when(service.changeQuantity(userId, orderId, request)).thenReturn(updatedOrder);
        when(mapper.toResponse(updatedOrder, user)).thenReturn(expected);

        OrderResponse actual = orchestrator.changeQuantity(userId, orderId, request);

        assertSame(expected, actual);
        verify(client).getById(userId);
        verify(service).changeQuantity(userId, orderId, request);
        verify(mapper).toResponse(updatedOrder, user);
    }

    @Test
    void updateStatusShouldLoadUserCallServiceAndMapResponse() {
        Long userId = 1L;
        Long orderId = 200L;

        UserResponse user = userResponse(userId);
        Order updatedOrder = order(orderId, userId);
        OrderResponse expected = orderResponse(updatedOrder, user);

        when(client.getById(userId)).thenReturn(user);
        when(service.updateStatus(userId, orderId, PAID)).thenReturn(updatedOrder);
        when(mapper.toResponse(updatedOrder, user)).thenReturn(expected);

        OrderResponse actual = orchestrator.updateStatus(userId, orderId, OrderStatus.PAID);

        assertSame(expected, actual);
        verify(client).getById(userId);
        verify(service).updateStatus(userId, orderId, PAID);
        verify(mapper).toResponse(updatedOrder, user);
    }

    @Test
    void deleteShouldDelegateToServiceOnly() {
        Long userId = 1L;
        Long orderId = 200L;

        orchestrator.delete(userId, orderId);

        verify(service).delete(userId, orderId);
        verify(client, never()).getById(any());
        verify(client, never()).getByIds(any());
        verify(mapper, never()).toResponse(any(), any());
    }

    @Test
    void restoreShouldLoadUserCallServiceAndMapResponse() {
        Long userId = 1L;
        Long orderId = 200L;

        UserResponse user = userResponse(userId);
        Order restoredOrder = order(orderId, userId);
        OrderResponse expected = orderResponse(restoredOrder, user);

        when(client.getById(userId)).thenReturn(user);
        when(service.restore(userId, orderId)).thenReturn(restoredOrder);
        when(mapper.toResponse(restoredOrder, user)).thenReturn(expected);

        OrderResponse actual = orchestrator.restore(userId, orderId);

        assertSame(expected, actual);
        verify(client).getById(userId);
        verify(service).restore(userId, orderId);
        verify(mapper).toResponse(restoredOrder, user);
    }

}
