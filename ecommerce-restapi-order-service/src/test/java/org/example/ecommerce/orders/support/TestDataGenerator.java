package org.example.ecommerce.orders.support;

import org.apache.commons.lang3.RandomStringUtils;
import org.example.ecommerce.orders.dto.request.ItemCreateRequest;
import org.example.ecommerce.orders.dto.request.ItemScrollRequest;
import org.example.ecommerce.orders.dto.request.ItemUpdateRequest;
import org.example.ecommerce.orders.dto.request.OrderAddItemRequest;
import org.example.ecommerce.orders.dto.request.OrderChangeQuantityRequest;
import org.example.ecommerce.orders.dto.request.OrderScrollRequest;
import org.example.ecommerce.orders.dto.response.ItemPageResponse;
import org.example.ecommerce.orders.dto.response.ItemResponse;
import org.example.ecommerce.orders.dto.response.OrderItemResponse;
import org.example.ecommerce.orders.dto.response.OrderPageResponse;
import org.example.ecommerce.orders.dto.response.OrderResponse;
import org.example.ecommerce.orders.dto.response.UserResponse;
import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.entity.Item_;
import org.example.ecommerce.orders.entity.Order;
import org.example.ecommerce.orders.entity.OrderItem;
import org.example.ecommerce.orders.entity.Order_;
import org.example.ecommerce.orders.enums.OrderStatus;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TestDataGenerator {

    private static final RandomStringUtils RSU = RandomStringUtils.insecure();

    private TestDataGenerator() {
        throw new AssertionError();
    }

    public static Long id() {
        return ThreadLocalRandom.current().nextLong(1, 10_000);
    }

    public static String itemName() {
        return "Item-".concat(RSU.nextAlphabetic(8));
    }

    public static BigDecimal price() {
        return BigDecimal.valueOf(
            ThreadLocalRandom.current().nextLong(100, 50_000),
            2
        );
    }

    public static LocalDate birthdate() {
        return LocalDate.now().minusYears(
            ThreadLocalRandom.current().nextInt(18, 60)
        );
    }

    public static LocalDateTime datetime() {
        return LocalDateTime.now().minusDays(
            ThreadLocalRandom.current().nextInt(1, 365)
        );
    }

    public static UserResponse userResponse() {
        return userResponse(id());
    }

    public static UserResponse userResponse(Long userId) {
        return new UserResponse(
            userId,
            "Name-".concat(RSU.nextAlphabetic(6)),
            "Surname-".concat(RSU.nextAlphabetic(8)),
            birthdate(),
            RSU.nextAlphabetic(10).toLowerCase() + "@test.com",
            true,
            datetime(),
            datetime()
        );
    }

    public static Item item() {
        return item(id());
    }

    public static Item item(Long itemId) {
        return item(itemId, itemName(), price());
    }

    public static Item item(Long itemId, String name, BigDecimal price) {
        Item item = new Item(name, price);
        setField(item, Item_.ID, itemId);
        setField(item, Item_.CREATED_AT, datetime());
        setField(item, Item_.UPDATED_AT, datetime());
        return item;
    }

    public static Item archivedItem(Long itemId) {
        Item item = item(itemId);
        item.archive();
        return item;
    }

    public static ItemCreateRequest itemCreateRequest() {
        return new ItemCreateRequest(
            itemName(),
            price()
        );
    }

    public static ItemUpdateRequest itemUpdateRequest() {
        return new ItemUpdateRequest(
            itemName(),
            price()
        );
    }

    public static ItemScrollRequest itemScrollRequest(Integer size, String token) {
        return new ItemScrollRequest(size, token);
    }

    public static ItemResponse itemResponse() {
        return itemResponse(item());
    }

    public static ItemResponse itemResponse(Item item) {
        return new ItemResponse(
            item.getId(),
            item.getName(),
            item.getPrice(),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }

    public static ItemPageResponse itemPageResponse(List<ItemResponse> items, String token) {
        return new ItemPageResponse(items, token);
    }

    public static LocalDateTime datetime(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    public static Item item(Long itemId, String name, BigDecimal price, LocalDateTime createdAt) {
        Item item = new Item(name, price);
        setField(item, Item_.ID, itemId);
        setField(item, Item_.CREATED_AT, createdAt);
        setField(item, Item_.UPDATED_AT, createdAt);
        return item;
    }

    public static Order order(Long orderId, Long userId, LocalDateTime createdAt) {
        Order order = new Order(userId);
        setField(order, Order_.ID, orderId);
        setField(order, Order_.CREATED_AT, createdAt);
        setField(order, Order_.UPDATED_AT, createdAt);
        setField(order, Order_.VERSION, 0L);
        return order;
    }

    public static Order order(Long userId) {
        return order(id(), userId);
    }

    public static Order order(Long orderId, Long userId) {
        Order order = new Order(userId);
        setField(order, Order_.ID, orderId);
        setField(order, Order_.CREATED_AT, datetime());
        setField(order, Order_.UPDATED_AT, datetime());
        setField(order, Order_.VERSION, 0L);
        return order;
    }

    public static Order orderWithItem(Long orderId, Long userId, Item item, int quantity) {
        Order order = order(orderId, userId);
        order.addItem(item, quantity);
        return order;
    }

    public static Order paidOrder(Long orderId, Long userId) {
        Order order = order(orderId, userId);
        order.addItem(item(), 1);
        order.markPaid();
        return order;
    }

    public static Order completedOrder(Long orderId, Long userId) {
        Order order = paidOrder(orderId, userId);
        order.complete();
        return order;
    }

    public static Order cancelledOrder(Long orderId, Long userId) {
        Order order = order(orderId, userId);
        order.cancel();
        return order;
    }

    public static Order deletedOrder(Long orderId, Long userId) {
        Order order = order(orderId, userId);
        order.markDeleted();
        return order;
    }

    public static OrderAddItemRequest orderAddItemRequest() {
        return orderAddItemRequest(id(), ThreadLocalRandom.current().nextInt(1, 5));
    }

    public static OrderAddItemRequest orderAddItemRequest(Long itemId, int quantity) {
        return new OrderAddItemRequest(itemId, quantity);
    }

    public static OrderChangeQuantityRequest orderChangeQuantityRequest(Long itemId, int quantity) {
        return new OrderChangeQuantityRequest(itemId, quantity);
    }

    public static OrderScrollRequest orderScrollRequest(Integer size,
                                                        OrderStatus status,
                                                        LocalDateTime from,
                                                        LocalDateTime to,
                                                        String token) {
        return new OrderScrollRequest(size, status, from, to, token);
    }

    public static OrderResponse orderResponse(Order order, UserResponse user) {
        return new OrderResponse(
            order.getId(),
            user,
            order.getStatus(),
            order.getTotalPrice(),
            order.isDeleted(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getOrderItems().stream()
                .map(TestDataGenerator::orderItemResponse)
                .toList()
        );
    }

    public static OrderPageResponse orderPageResponse(List<OrderResponse> orders, String token) {
        return new OrderPageResponse(orders, token);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "Failed to set field '%s' on %s".formatted(
                    fieldName,
                    target.getClass().getSimpleName()
                ),
                e
            );
        }
    }

    private static OrderItemResponse orderItemResponse(OrderItem orderItem) {
        Item item = orderItem.getItem();

        return new OrderItemResponse(
            orderItem.getId(),
            item.getId(),
            item.getName(),
            item.getPrice(),
            orderItem.getQuantity(),
            item.getPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity())),
            orderItem.getCreatedAt(),
            orderItem.getUpdatedAt()
        );
    }

}
