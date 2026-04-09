package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.client.UserClient;
import org.example.ecommerce.orders.dto.request.CursorPayload;
import org.example.ecommerce.orders.dto.request.OrderAddItemRequest;
import org.example.ecommerce.orders.dto.request.OrderChangeQuantityRequest;
import org.example.ecommerce.orders.dto.request.OrderScrollRequest;
import org.example.ecommerce.orders.dto.response.OrderPageResponse;
import org.example.ecommerce.orders.dto.response.OrderResponse;
import org.example.ecommerce.orders.dto.response.UserResponse;
import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.enums.OrderStatus;
import org.example.ecommerce.orders.exception.custom.item.ItemNotFoundException;
import org.example.ecommerce.orders.exception.custom.order.OrderNotFoundException;
import org.example.ecommerce.orders.exception.custom.order.OrderStateConflictException;
import org.example.ecommerce.orders.exception.custom.order.OrderStatusInvalidException;
import org.example.ecommerce.orders.mapper.OrderMapper;
import org.example.ecommerce.orders.repository.ItemRepository;
import org.example.ecommerce.orders.repository.OrderRepository;
import org.example.ecommerce.orders.repository.pagination.OrderSpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class OrderService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final UserClient userClient;
    private final OrderMapper orderMapper;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final CursorService cursorService;
    private final OrderSpecificationBuilder orderSpecificationBuilder;

    public OrderService(UserClient userClient,
                        OrderMapper orderMapper,
                        ItemRepository itemRepository,
                        OrderRepository orderRepository,
                        CursorService cursorService,
                        OrderSpecificationBuilder orderSpecificationBuilder) {
        this.userClient = userClient;
        this.orderMapper = orderMapper;
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
        this.cursorService = cursorService;
        this.orderSpecificationBuilder = orderSpecificationBuilder;
    }

    @Transactional
    public OrderResponse create(Long userId) {
        UserResponse user = userClient.getById(userId);
        Order saved = orderRepository.save(new Order(userId));

        return orderMapper.toResponse(saved, user);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, Long orderId) {
        UserResponse user = userClient.getById(userId);
        Order order = getMyOrder(userId, orderId);

        return orderMapper.toResponse(order, user);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse getMy(Long userId, OrderScrollRequest request) {
        Specification<Order> specification = orderSpecificationBuilder.buildMyOrders(userId, request);
        return getPage(request, specification, orderIds -> orderRepository.findPage(userId, orderIds));
    }

    @Transactional(readOnly = true)
    public OrderPageResponse getAll(OrderScrollRequest request) {
        Specification<Order> specification = orderSpecificationBuilder.buildAllOrders(request);
        return getPage(request, specification, orderRepository::findPage);
    }

    private OrderPageResponse getPage(OrderScrollRequest request,
                                      Specification<Order> specification,
                                      Function<List<Long>, List<Order>> ordersFetcher) {
        int pageSize = normalizeSize(request.size());
        CursorPayload cursor = cursorService.decode(request.token());

        List<Long> pageIds = orderRepository.findPageIds(
            specification,
            cursor.createdAt(),
            cursor.id(),
            pageSize + 1
        );

        boolean hasNext = pageIds.size() > pageSize;
        if (hasNext) {
            pageIds = pageIds.subList(0, pageSize);
        }

        if (pageIds.isEmpty()) {
            return OrderPageResponse.empty();
        }

        List<Order> orders = ordersFetcher.apply(pageIds);
        sortOrders(orders, pageIds);

        Map<Long, UserResponse> users = loadUsers(orders);

        List<OrderResponse> responses = orders.stream()
            .map(order -> orderMapper.toResponse(order, users.get(order.getUserId())))
            .toList();

        String nextToken = null;
        if (hasNext) {
            Order lastOrder = orders.getLast();
            nextToken = cursorService.encode(
                new CursorPayload(lastOrder.getCreatedAt(), lastOrder.getId())
            );
        }

        return new OrderPageResponse(responses, nextToken);
    }

    @Transactional
    public OrderResponse addItem(Long userId, Long orderId, OrderAddItemRequest request) {
        UserResponse user = userClient.getById(userId);

        Order order = getMyOrder(userId, orderId);
        ensureEditable(order);

        Item item = itemRepository.findById(request.itemId())
            .orElseThrow(() -> new ItemNotFoundException(request.itemId()));

        order.addItem(item, request.quantity());
        orderRepository.flush();

        return orderMapper.toResponse(order, user);
    }

    @Transactional
    public OrderResponse removeItem(Long userId, Long orderId, Long itemId) {
        UserResponse user = userClient.getById(userId);

        Order order = getMyOrder(userId, orderId);
        ensureEditable(order);

        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException(itemId));

        order.removeItem(item);
        orderRepository.flush();

        return orderMapper.toResponse(order, user);
    }

    @Transactional
    public OrderResponse changeQuantity(Long userId, Long orderId, OrderChangeQuantityRequest request) {
        UserResponse user = userClient.getById(userId);

        Order order = getMyOrder(userId, orderId);
        ensureEditable(order);

        Item item = itemRepository.findById(request.itemId())
            .orElseThrow(() -> new ItemNotFoundException(request.itemId()));

        order.changeItemQuantity(item, request.quantity());
        orderRepository.flush();

        return orderMapper.toResponse(order, user);
    }

    @Transactional
    public OrderResponse updateStatus(Long userId, Long orderId, OrderStatus status) {
        UserResponse user = userClient.getById(userId);
        Order order = getMyOrder(userId, orderId);

        switch (status) {
            case PAID -> order.markPaid();
            case COMPLETED -> order.complete();
            case CANCELLED -> order.cancel();
            case NEW -> throw new OrderStatusInvalidException("Status NEW is not allowed for manual update");
        }

        orderRepository.flush();
        return orderMapper.toResponse(order, user);
    }

    @Transactional
    public void delete(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId))
            throw new OrderNotFoundException(orderId);

        order.markDeleted();
    }

    @Transactional
    public OrderResponse restore(Long userId, Long orderId) {
        UserResponse user = userClient.getById(userId);

        Order order = orderRepository.findDeleted(orderId, userId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.restore();
        orderRepository.flush();

        return orderMapper.toResponse(order, user);
    }

    private Order getMyOrder(Long userId, Long orderId) {
        return orderRepository.findDetailed(orderId, userId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void ensureEditable(Order order) {
        if (!order.isNew())
            throw new OrderStateConflictException("Only NEW order can be modified");
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 0)
            return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void sortOrders(List<Order> orders, List<Long> pageIds) {
        Map<Long, Integer> positions = new HashMap<>();
        for (int i = 0; i < pageIds.size(); i++)
            positions.put(pageIds.get(i), i);

        orders.sort(Comparator.comparingInt(order -> positions.get(order.getId())));
    }

    private Map<Long, UserResponse> loadUsers(List<Order> orders) {
        Map<Long, UserResponse> users = new HashMap<>();

        for (Order order : orders) {
            users.computeIfAbsent(order.getUserId(), userClient::getById);
        }

        return users;
    }

}
