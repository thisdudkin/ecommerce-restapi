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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderOrchestrator {

    private final UserClient client;
    private final OrderMapper mapper;
    private final OrderService service;

    public OrderOrchestrator(UserClient client, OrderMapper mapper, OrderService service) {
        this.client = client;
        this.mapper = mapper;
        this.service = service;
    }

    public OrderResponse create(Long userId) {
        var user = client.getById(userId);
        var order = service.create(userId);
        return mapper.toResponse(order, user);
    }

    public OrderResponse get(Long userId, Long orderId) {
        var user = client.getById(userId);
        var order = service.get(userId, orderId);
        return mapper.toResponse(order, user);
    }

    public OrderPageResponse getMy(Long userId, OrderScrollRequest request) {
        var page = service.getMy(userId, request);
        return toPageResponse(page);
    }

    public OrderPageResponse getAll(OrderScrollRequest request) {
        var page = service.getAll(request);
        return toPageResponse(page);
    }

    public OrderResponse addItem(Long userId, Long orderId, OrderAddItemRequest request) {
        var user = client.getById(userId);
        var order = service.addItem(userId, orderId, request);
        return mapper.toResponse(order, user);
    }

    public OrderResponse removeItem(Long userId, Long orderId, Long itemId) {
        var user = client.getById(userId);
        var order = service.removeItem(userId, orderId, itemId);
        return mapper.toResponse(order, user);
    }

    public OrderResponse changeQuantity(Long userId, Long orderId, OrderChangeQuantityRequest request) {
        var user = client.getById(userId);
        var order = service.changeQuantity(userId, orderId, request);
        return mapper.toResponse(order, user);
    }

    public OrderResponse updateStatus(Long userId, Long orderId, OrderStatus status) {
        var user = client.getById(userId);
        var order = service.updateStatus(userId, orderId, status);
        return mapper.toResponse(order, user);
    }

    public void delete(Long userId, Long orderId) {
        service.delete(userId, orderId);
    }

    public OrderResponse restore(Long userId, Long orderId) {
        var user = client.getById(userId);
        var order = service.restore(userId, orderId);
        return mapper.toResponse(order, user);
    }

    private OrderPageResponse toPageResponse(OrderPageData page) {
        if (page.orders().isEmpty()) {
            return OrderPageResponse.empty();
        }

        Map<Long, UserResponse> users = loadUsers(page.orders());

        List<OrderResponse> responses = page.orders().stream()
            .map(order -> mapper.toResponse(order, users.get(order.getUserId())))
            .toList();

        return new OrderPageResponse(responses, page.token());
    }

    private Map<Long, UserResponse> loadUsers(List<Order> orders) {
        Set<Long> userIds = orders.stream()
            .map(Order::getUserId)
            .collect(Collectors.toSet());

        Map<Long, UserResponse> users = client.getByIds(userIds).stream()
            .collect(Collectors.toMap(UserResponse::id, Function.identity()));

        List<Long> missingIds = userIds.stream()
            .filter(id -> !users.containsKey(id))
            .toList();

        if (!missingIds.isEmpty()) {
            throw new UserNotFoundException("Users not found: " + missingIds);
        }

        return users;
    }

}
