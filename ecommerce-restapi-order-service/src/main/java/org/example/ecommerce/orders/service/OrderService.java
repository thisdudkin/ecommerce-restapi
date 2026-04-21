package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.dto.request.CursorPayload;
import org.example.ecommerce.orders.dto.request.OrderAddItemRequest;
import org.example.ecommerce.orders.dto.request.OrderChangeQuantityRequest;
import org.example.ecommerce.orders.dto.request.OrderScrollRequest;
import org.example.ecommerce.orders.dto.response.OrderPageData;
import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.enums.OrderStatus;
import org.example.ecommerce.orders.exception.custom.item.ItemNotFoundException;
import org.example.ecommerce.orders.exception.custom.order.OrderNotFoundException;
import org.example.ecommerce.orders.exception.custom.order.OrderStateConflictException;
import org.example.ecommerce.orders.exception.custom.order.OrderStatusInvalidException;
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

    private final CursorService cursorService;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OrderSpecificationBuilder specificationBuilder;

    public OrderService(CursorService cursorService,
                        ItemRepository itemRepository,
                        OrderRepository orderRepository,
                        OrderSpecificationBuilder specificationBuilder) {
        this.cursorService = cursorService;
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
        this.specificationBuilder = specificationBuilder;
    }

    @Transactional
    public Order create(Long userId) {
        return orderRepository.save(new Order(userId));
    }

    @Transactional(readOnly = true)
    public Order get(Long userId, Long orderId) {
        return getMyOrder(userId, orderId);
    }

    @Transactional(readOnly = true)
    public OrderPageData getMy(Long userId, OrderScrollRequest request) {
        Specification<Order> specification = specificationBuilder.buildMyOrders(userId, request);
        return getPage(request, specification, orderIds -> orderRepository.findPage(userId, orderIds));
    }

    @Transactional(readOnly = true)
    public OrderPageData getAll(OrderScrollRequest request) {
        Specification<Order> specification = specificationBuilder.buildAllOrders(request);
        return getPage(request, specification, orderRepository::findPage);
    }

    @Transactional
    public Order addItem(Long userId, Long orderId, OrderAddItemRequest request) {
        Order order = getMyOrder(userId, orderId);
        ensureEditable(order);

        Item item = itemRepository.findById(request.itemId())
            .orElseThrow(() -> new ItemNotFoundException(request.itemId()));

        order.addItem(item, request.quantity());
        orderRepository.flush();

        return order;
    }

    @Transactional
    public Order removeItem(Long userId, Long orderId, Long itemId) {
        Order order = getMyOrder(userId, orderId);
        ensureEditable(order);

        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException(itemId));

        order.removeItem(item);
        orderRepository.flush();

        return order;
    }

    @Transactional
    public Order changeQuantity(Long userId, Long orderId, OrderChangeQuantityRequest request) {
        Order order = getMyOrder(userId, orderId);
        ensureEditable(order);

        Item item = itemRepository.findById(request.itemId())
            .orElseThrow(() -> new ItemNotFoundException(request.itemId()));

        order.changeItemQuantity(item, request.quantity());
        orderRepository.flush();

        return order;
    }

    @Transactional
    public Order updateStatus(Long userId, Long orderId, OrderStatus status) {
        Order order = getMyOrder(userId, orderId);

        switch (status) {
            case PAID -> order.markPaid();
            case COMPLETED -> order.complete();
            case CANCELLED -> order.cancel();
            case NEW -> throw new OrderStatusInvalidException("Status NEW is not allowed for manual update");
        }

        orderRepository.flush();
        return order;
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
    public Order restore(Long userId, Long orderId) {
        Order order = orderRepository.findDeleted(orderId, userId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.restore();
        orderRepository.flush();

        return order;
    }

    private Order getMyOrder(Long userId, Long orderId) {
        return orderRepository.findDetailed(orderId, userId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private OrderPageData getPage(OrderScrollRequest request,
                                  Specification<Order> specification,
                                  Function<List<Long>, List<Order>> fetcher) {
        int size = normalizeSize(request.size());
        CursorPayload cursor = cursorService.decode(request.token());

        List<Long> pageIds = orderRepository.findPageIds(
            specification,
            cursor.createdAt(),
            cursor.id(),
            size + 1
        );

        boolean hasNext = pageIds.size() > size;
        if (hasNext)
            pageIds = pageIds.subList(0, size);

        if (pageIds.isEmpty())
            return OrderPageData.empty();

        List<Order> orders = fetcher.apply(pageIds);
        sortOrders(orders, pageIds);

        String nextToken = null;
        if (hasNext) {
            Order last = orders.getLast();
            nextToken = cursorService.encode(
                new CursorPayload(last.getCreatedAt(), last.getId())
            );
        }

        return new OrderPageData(orders, nextToken);
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

}
