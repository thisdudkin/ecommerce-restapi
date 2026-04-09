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
import org.example.ecommerce.orders.exception.custom.order.EmptyOrderException;
import org.example.ecommerce.orders.exception.custom.order.OrderNotFoundException;
import org.example.ecommerce.orders.exception.custom.order.OrderStateConflictException;
import org.example.ecommerce.orders.exception.custom.order.OrderStatusInvalidException;
import org.example.ecommerce.orders.mapper.OrderMapper;
import org.example.ecommerce.orders.repository.ItemRepository;
import org.example.ecommerce.orders.repository.OrderRepository;
import org.example.ecommerce.orders.repository.pagination.OrderSpecificationBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.example.ecommerce.orders.support.TestDataGenerator.datetime;
import static org.example.ecommerce.orders.support.TestDataGenerator.deletedOrder;
import static org.example.ecommerce.orders.support.TestDataGenerator.id;
import static org.example.ecommerce.orders.support.TestDataGenerator.item;
import static org.example.ecommerce.orders.support.TestDataGenerator.order;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderAddItemRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderChangeQuantityRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderResponse;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderScrollRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderWithItem;
import static org.example.ecommerce.orders.support.TestDataGenerator.paidOrder;
import static org.example.ecommerce.orders.support.TestDataGenerator.userResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private UserClient userClient;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CursorService cursorService;

    @Mock
    private OrderSpecificationBuilder orderSpecificationBuilder;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createShouldCreateOrderAndReturnResponse() {
        Long userId = id();
        UserResponse user = userResponse(userId);
        Order savedOrder = order(userId);
        OrderResponse expected = orderResponse(savedOrder, user);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.save(any(Order.class)))
            .thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder, user))
            .thenReturn(expected);

        OrderResponse actual = orderService.create(userId);

        assertEquals(expected, actual);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order toSave = captor.getValue();
        assertEquals(userId, toSave.getUserId());
        assertTrue(toSave.isNew());

        verify(userClient).getById(userId);
        verify(orderMapper).toResponse(savedOrder, user);
    }

    @Test
    void getShouldReturnOwnedOrder() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Order existingOrder = order(orderId, userId);
        OrderResponse expected = orderResponse(existingOrder, user);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(orderMapper.toResponse(existingOrder, user))
            .thenReturn(expected);

        OrderResponse actual = orderService.get(userId, orderId);

        assertEquals(expected, actual);
        verify(userClient).getById(userId);
        verify(orderRepository).findDetailed(orderId, userId);
        verify(orderMapper).toResponse(existingOrder, user);
    }

    @Test
    void getShouldThrowWhenOrderNotFound() {
        Long userId = id();
        Long orderId = id();
        UserResponse user = userResponse(userId);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.get(userId, orderId));

        verify(userClient).getById(userId);
        verify(orderRepository).findDetailed(orderId, userId);
        verifyNoInteractions(orderMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMyShouldReturnEmptyPageWhenNoIdsFoundAndUseDefaultSize() {
        Long userId = id();
        OrderScrollRequest request = orderScrollRequest(null, null, null, null, null);
        CursorPayload cursor = new CursorPayload(null, null);
        Specification<Order> specification = mock(Specification.class);

        when(cursorService.decode(null))
            .thenReturn(cursor);
        when(orderSpecificationBuilder.buildMyOrders(userId, request))
            .thenReturn(specification);
        when(orderRepository.findPageIds(specification, null, null, 21))
            .thenReturn(List.of());

        OrderPageResponse actual = orderService.getMy(userId, request);

        assertEquals(OrderPageResponse.empty(), actual);

        verify(cursorService).decode(null);
        verify(orderSpecificationBuilder).buildMyOrders(userId, request);
        verify(orderRepository).findPageIds(specification, null, null, 21);
        verify(orderRepository, never()).findPage(anyLong(), any());
        verify(cursorService, never()).encode(any());
        verifyNoInteractions(userClient, orderMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMyShouldCapPageSizeAtMax() {
        Long userId = id();
        OrderScrollRequest request = orderScrollRequest(999, null, null, null, "cursor");
        CursorPayload cursor = new CursorPayload(null, null);
        Specification<Order> specification = mock(Specification.class);

        when(cursorService.decode("cursor"))
            .thenReturn(cursor);
        when(orderSpecificationBuilder.buildMyOrders(userId, request))
            .thenReturn(specification);
        when(orderRepository.findPageIds(specification, null, null, 51))
            .thenReturn(List.of());

        OrderPageResponse actual = orderService.getMy(userId, request);

        assertEquals(OrderPageResponse.empty(), actual);

        verify(cursorService).decode("cursor");
        verify(orderSpecificationBuilder).buildMyOrders(userId, request);
        verify(orderRepository).findPageIds(specification, null, null, 51);
        verify(orderRepository, never()).findPage(anyLong(), any());
        verify(cursorService, never()).encode(any());
        verifyNoInteractions(userClient, orderMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMyShouldReturnSortedOrdersAndNextToken() {
        Long userId = id();
        UserResponse user = userResponse(userId);

        OrderScrollRequest request = orderScrollRequest(2, null, null, null, "cursor-1");
        CursorPayload cursor = new CursorPayload(datetime(), 77L);
        Specification<Order> specification = mock(Specification.class);

        Order firstOrder = order(101L, userId);
        Order secondOrder = order(102L, userId);

        OrderResponse firstResponse = orderResponse(firstOrder, user);
        OrderResponse secondResponse = orderResponse(secondOrder, user);

        List<Long> pageIds = List.of(firstOrder.getId(), secondOrder.getId(), 999L);
        List<Order> fetchedOrders = new ArrayList<>(List.of(secondOrder, firstOrder));

        when(userClient.getById(userId))
            .thenReturn(user);
        when(cursorService.decode("cursor-1"))
            .thenReturn(cursor);
        when(orderSpecificationBuilder.buildMyOrders(userId, request))
            .thenReturn(specification);
        when(orderRepository.findPageIds(specification, cursor.createdAt(), cursor.id(), 3))
            .thenReturn(pageIds);
        when(orderRepository.findPage(userId, List.of(firstOrder.getId(), secondOrder.getId())))
            .thenReturn(fetchedOrders);
        when(orderMapper.toResponse(firstOrder, user))
            .thenReturn(firstResponse);
        when(orderMapper.toResponse(secondOrder, user))
            .thenReturn(secondResponse);
        when(cursorService.encode(new CursorPayload(secondOrder.getCreatedAt(), secondOrder.getId())))
            .thenReturn("next-token");

        OrderPageResponse actual = orderService.getMy(userId, request);

        assertEquals(List.of(firstResponse, secondResponse), actual.orders());
        assertEquals("next-token", actual.token());

        verify(orderRepository).findPageIds(specification, cursor.createdAt(), cursor.id(), 3);
        verify(orderRepository).findPage(userId, List.of(firstOrder.getId(), secondOrder.getId()));
        verify(orderMapper).toResponse(firstOrder, user);
        verify(orderMapper).toResponse(secondOrder, user);
        verify(cursorService).encode(new CursorPayload(secondOrder.getCreatedAt(), secondOrder.getId()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllShouldReturnEmptyPageWhenNoIdsFound() {
        OrderScrollRequest request = orderScrollRequest(null, null, null, null, null);
        CursorPayload cursor = new CursorPayload(null, null);
        Specification<Order> specification = mock(Specification.class);

        when(cursorService.decode(null)).thenReturn(cursor);
        when(orderSpecificationBuilder.buildAllOrders(request)).thenReturn(specification);
        when(orderRepository.findPageIds(specification, null, null, 21)).thenReturn(List.of());

        OrderPageResponse actual = orderService.getAll(request);

        assertEquals(OrderPageResponse.empty(), actual);

        verify(orderSpecificationBuilder).buildAllOrders(request);
        verify(orderRepository).findPageIds(specification, null, null, 21);
        verify(orderRepository, never()).findPage(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllShouldReturnSortedOrdersAndNextToken() {
        Long firstUserId = 1L;
        Long secondUserId = 2L;

        OrderScrollRequest request = orderScrollRequest(2, null, null, null, "cursor-1");
        CursorPayload cursor = new CursorPayload(datetime(), 77L);
        Specification<Order> specification = mock(Specification.class);

        Order firstOrder = order(101L, firstUserId);
        Order secondOrder = order(102L, secondUserId);

        UserResponse firstUser = userResponse(firstUserId);
        UserResponse secondUser = userResponse(secondUserId);

        OrderResponse firstResponse = orderResponse(firstOrder, firstUser);
        OrderResponse secondResponse = orderResponse(secondOrder, secondUser);

        when(cursorService.decode("cursor-1")).thenReturn(cursor);
        when(orderSpecificationBuilder.buildAllOrders(request)).thenReturn(specification);
        when(orderRepository.findPageIds(specification, cursor.createdAt(), cursor.id(), 3))
            .thenReturn(List.of(101L, 102L, 999L));
        when(orderRepository.findPage(List.of(101L, 102L)))
            .thenReturn(new ArrayList<>(List.of(secondOrder, firstOrder)));

        when(userClient.getById(firstUserId)).thenReturn(firstUser);
        when(userClient.getById(secondUserId)).thenReturn(secondUser);

        when(orderMapper.toResponse(firstOrder, firstUser)).thenReturn(firstResponse);
        when(orderMapper.toResponse(secondOrder, secondUser)).thenReturn(secondResponse);

        when(cursorService.encode(new CursorPayload(secondOrder.getCreatedAt(), secondOrder.getId())))
            .thenReturn("next-token");

        OrderPageResponse actual = orderService.getAll(request);

        assertEquals(List.of(firstResponse, secondResponse), actual.orders());
        assertEquals("next-token", actual.token());
    }

    @Test
    void addItemShouldAddItemToEditableOrderFlushAndReturnResponse() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Order existingOrder = order(orderId, userId);
        Item existingItem = item(501L);
        OrderAddItemRequest request = orderAddItemRequest(existingItem.getId(), 3);
        OrderResponse expected = orderResponse(order(orderId, userId), user);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(existingItem.getId()))
            .thenReturn(Optional.of(existingItem));
        when(orderMapper.toResponse(existingOrder, user))
            .thenReturn(expected);

        OrderResponse actual = orderService.addItem(userId, orderId, request);

        assertEquals(expected, actual);
        assertTrue(existingOrder.hasItem(existingItem));
        assertEquals(1, existingOrder.getOrderItems().size());
        assertEquals(3, existingOrder.getOrderItems().getFirst().getQuantity());
        assertEquals(
            existingItem.getPrice()
                .multiply(BigDecimal.valueOf(3)),
            existingOrder.getTotalPrice()
        );

        verify(userClient).getById(userId);
        verify(orderRepository).findDetailed(orderId, userId);
        verify(itemRepository).findById(existingItem.getId());
        verify(orderRepository).flush();
        verify(orderMapper).toResponse(existingOrder, user);
    }

    @Test
    void addItemShouldThrowWhenOrderIsNotEditable() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Order paidOrder = paidOrder(orderId, userId);
        OrderAddItemRequest request = orderAddItemRequest(id(), 2);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(paidOrder));

        assertThrows(
            OrderStateConflictException.class,
            () -> orderService.addItem(userId, orderId, request)
        );

        verify(userClient).getById(userId);
        verify(orderRepository).findDetailed(orderId, userId);
        verifyNoInteractions(itemRepository, orderMapper);
        verify(orderRepository, never()).flush();
    }

    @Test
    void addItemShouldThrowWhenItemNotFound() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Order existingOrder = order(orderId, userId);
        OrderAddItemRequest request = orderAddItemRequest(999L, 2);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(
            ItemNotFoundException.class,
            () -> orderService.addItem(userId, orderId, request)
        );

        verify(itemRepository).findById(999L);
        verify(orderRepository, never()).flush();
        verifyNoInteractions(orderMapper);
    }

    @Test
    void removeItemShouldRemoveExistingItemFlushAndReturnResponse() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Item existingItem = item(701L);
        Order existingOrder = orderWithItem(orderId, userId, existingItem, 2);
        OrderResponse expected = orderResponse(order(orderId, userId), user);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(existingItem.getId()))
            .thenReturn(Optional.of(existingItem));
        when(orderMapper.toResponse(existingOrder, user))
            .thenReturn(expected);

        OrderResponse actual = orderService.removeItem(userId, orderId, existingItem.getId());

        assertEquals(expected, actual);
        assertFalse(existingOrder.hasItem(existingItem));
        assertTrue(existingOrder.getOrderItems().isEmpty());
        assertEquals(BigDecimal.ZERO, existingOrder.getTotalPrice());

        verify(itemRepository).findById(existingItem.getId());
        verify(orderRepository).flush();
        verify(orderMapper).toResponse(existingOrder, user);
    }

    @Test
    void changeQuantityShouldUpdateExistingOrderItemFlushAndReturnResponse() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Item existingItem = item(801L);
        Order existingOrder = orderWithItem(orderId, userId, existingItem, 1);
        OrderChangeQuantityRequest request = orderChangeQuantityRequest(existingItem.getId(), 5);
        OrderResponse expected = orderResponse(order(orderId, userId), user);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(existingItem.getId()))
            .thenReturn(Optional.of(existingItem));
        when(orderMapper.toResponse(existingOrder, user))
            .thenReturn(expected);

        OrderResponse actual = orderService.changeQuantity(userId, orderId, request);

        assertEquals(expected, actual);
        assertEquals(1, existingOrder.getOrderItems().size());
        assertEquals(5, existingOrder.getOrderItems().getFirst().getQuantity());
        assertEquals(
            existingItem.getPrice()
                .multiply(BigDecimal.valueOf(5)),
            existingOrder.getTotalPrice()
        );

        verify(itemRepository).findById(existingItem.getId());
        verify(orderRepository).flush();
        verify(orderMapper).toResponse(existingOrder, user);
    }

    @Test
    void updateStatusShouldMarkOrderPaidFlushAndReturnResponse() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Item existingItem = item(901L);
        Order existingOrder = orderWithItem(orderId, userId, existingItem, 2);
        OrderResponse expected = orderResponse(order(orderId, userId), user);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(orderMapper.toResponse(existingOrder, user))
            .thenReturn(expected);

        OrderResponse actual = orderService.updateStatus(userId, orderId, OrderStatus.PAID);

        assertEquals(expected, actual);
        assertTrue(existingOrder.isPaid());

        verify(orderRepository).flush();
        verify(orderMapper).toResponse(existingOrder, user);
    }

    @Test
    void updateStatusShouldThrowWhenPayingEmptyOrder() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Order existingOrder = order(orderId, userId);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));

        assertThrows(
            EmptyOrderException.class,
            () -> orderService.updateStatus(userId, orderId, OrderStatus.PAID)
        );

        verify(orderRepository, never()).flush();
        verifyNoInteractions(orderMapper);
    }

    @Test
    void updateStatusShouldMarkPaidOrderCompletedFlushAndReturnResponse() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Order existingOrder = paidOrder(orderId, userId);
        OrderResponse expected = orderResponse(order(orderId, userId), user);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(orderMapper.toResponse(existingOrder, user))
            .thenReturn(expected);

        OrderResponse actual = orderService.updateStatus(userId, orderId, OrderStatus.COMPLETED);

        assertEquals(expected, actual);
        assertTrue(existingOrder.isCompleted());

        verify(orderRepository).flush();
        verify(orderMapper).toResponse(existingOrder, user);
    }

    @Test
    void updateStatusShouldMarkOrderCancelledFlushAndReturnResponse() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Order existingOrder = order(orderId, userId);
        OrderResponse expected = orderResponse(order(orderId, userId), user);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(orderMapper.toResponse(existingOrder, user))
            .thenReturn(expected);

        OrderResponse actual = orderService.updateStatus(userId, orderId, OrderStatus.CANCELLED);

        assertEquals(expected, actual);
        assertTrue(existingOrder.isCancelled());

        verify(orderRepository).flush();
        verify(orderMapper).toResponse(existingOrder, user);
    }

    @Test
    void updateStatusShouldThrowWhenStatusIsNew() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Order existingOrder = order(orderId, userId);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));

        assertThrows(
            OrderStatusInvalidException.class,
            () -> orderService.updateStatus(userId, orderId, OrderStatus.NEW)
        );

        verify(orderRepository, never()).flush();
        verifyNoInteractions(orderMapper);
    }

    @Test
    void deleteShouldMarkOrderDeletedWhenItBelongsToUser() {
        Long userId = id();
        Long orderId = id();

        Order existingOrder = order(orderId, userId);

        when(orderRepository.findById(orderId))
            .thenReturn(Optional.of(existingOrder));

        orderService.delete(userId, orderId);

        assertTrue(existingOrder.isDeleted());
        verify(orderRepository).findById(orderId);
        verifyNoInteractions(userClient, itemRepository, orderMapper, cursorService, orderSpecificationBuilder);
    }

    @Test
    void deleteShouldThrowWhenOrderBelongsToAnotherUser() {
        Long userId = id();
        Long orderId = id();

        Order existingOrder = order(orderId, userId + 1);

        when(orderRepository.findById(orderId))
            .thenReturn(Optional.of(existingOrder));

        assertThrows(
            OrderNotFoundException.class,
            () -> orderService.delete(userId, orderId)
        );

        assertFalse(existingOrder.isDeleted());
        verify(orderRepository).findById(orderId);
        verifyNoInteractions(userClient, itemRepository, orderMapper, cursorService, orderSpecificationBuilder);
    }

    @Test
    void restoreShouldRestoreDeletedOrderFlushAndReturnResponse() {
        Long userId = id();
        Long orderId = id();

        UserResponse user = userResponse(userId);
        Order deletedOrder = deletedOrder(orderId, userId);
        OrderResponse expected = orderResponse(order(orderId, userId), user);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDeleted(orderId, userId))
            .thenReturn(Optional.of(deletedOrder));
        when(orderMapper.toResponse(deletedOrder, user))
            .thenReturn(expected);

        OrderResponse actual = orderService.restore(userId, orderId);

        assertEquals(expected, actual);
        assertFalse(deletedOrder.isDeleted());

        verify(userClient).getById(userId);
        verify(orderRepository).findDeleted(orderId, userId);
        verify(orderRepository).flush();
        verify(orderMapper).toResponse(deletedOrder, user);
    }

    @Test
    void restoreShouldThrowWhenDeletedOrderNotFound() {
        Long userId = id();
        Long orderId = id();
        UserResponse user = userResponse(userId);

        when(userClient.getById(userId))
            .thenReturn(user);
        when(orderRepository.findDeleted(orderId, userId))
            .thenReturn(Optional.empty());

        assertThrows(
            OrderNotFoundException.class,
            () -> orderService.restore(userId, orderId)
        );

        verify(userClient).getById(userId);
        verify(orderRepository).findDeleted(orderId, userId);
        verify(orderRepository, never()).flush();
        verifyNoInteractions(orderMapper);
    }

}
