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
import org.example.ecommerce.orders.exception.custom.order.EmptyOrderException;
import org.example.ecommerce.orders.exception.custom.order.OrderNotFoundException;
import org.example.ecommerce.orders.exception.custom.order.OrderStateConflictException;
import org.example.ecommerce.orders.exception.custom.order.OrderStatusInvalidException;
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
import static org.example.ecommerce.orders.support.TestDataGenerator.orderScrollRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.orderWithItem;
import static org.example.ecommerce.orders.support.TestDataGenerator.paidOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void createShouldCreateOrderAndReturnOrder() {
        Long userId = id();
        Order savedOrder = order(userId);

        when(orderRepository.save(any(Order.class)))
            .thenReturn(savedOrder);

        Order actual = orderService.create(userId);

        assertSame(savedOrder, actual);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order toSave = captor.getValue();
        assertEquals(userId, toSave.getUserId());
        assertTrue(toSave.isNew());
    }

    @Test
    void getShouldReturnOwnedOrder() {
        Long userId = id();
        Long orderId = id();

        Order existingOrder = order(orderId, userId);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));

        Order actual = orderService.get(userId, orderId);

        assertSame(existingOrder, actual);
        verify(orderRepository).findDetailed(orderId, userId);
    }

    @Test
    void getShouldThrowWhenOrderNotFound() {
        Long userId = id();
        Long orderId = id();

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.get(userId, orderId));

        verify(orderRepository).findDetailed(orderId, userId);
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

        OrderPageData actual = orderService.getMy(userId, request);

        assertTrue(actual.orders().isEmpty());
        assertNull(actual.token());

        verify(cursorService).decode(null);
        verify(orderSpecificationBuilder).buildMyOrders(userId, request);
        verify(orderRepository).findPageIds(specification, null, null, 21);
        verify(orderRepository, never()).findPage(anyLong(), any());
        verify(cursorService, never()).encode(any());
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

        OrderPageData actual = orderService.getMy(userId, request);

        assertTrue(actual.orders().isEmpty());
        assertNull(actual.token());

        verify(cursorService).decode("cursor");
        verify(orderSpecificationBuilder).buildMyOrders(userId, request);
        verify(orderRepository).findPageIds(specification, null, null, 51);
        verify(orderRepository, never()).findPage(anyLong(), any());
        verify(cursorService, never()).encode(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMyShouldReturnSortedOrdersAndNextToken() {
        Long userId = id();

        OrderScrollRequest request = orderScrollRequest(2, null, null, null, "cursor-1");
        CursorPayload cursor = new CursorPayload(datetime(), 77L);
        Specification<Order> specification = mock(Specification.class);

        Order firstOrder = order(101L, userId);
        Order secondOrder = order(102L, userId);

        List<Long> pageIds = List.of(firstOrder.getId(), secondOrder.getId(), 999L);
        List<Order> fetchedOrders = new ArrayList<>(List.of(secondOrder, firstOrder));

        when(cursorService.decode("cursor-1"))
            .thenReturn(cursor);
        when(orderSpecificationBuilder.buildMyOrders(userId, request))
            .thenReturn(specification);
        when(orderRepository.findPageIds(specification, cursor.createdAt(), cursor.id(), 3))
            .thenReturn(pageIds);
        when(orderRepository.findPage(userId, List.of(firstOrder.getId(), secondOrder.getId())))
            .thenReturn(fetchedOrders);
        when(cursorService.encode(new CursorPayload(secondOrder.getCreatedAt(), secondOrder.getId())))
            .thenReturn("next-token");

        OrderPageData actual = orderService.getMy(userId, request);

        assertEquals(List.of(firstOrder, secondOrder), actual.orders());
        assertEquals("next-token", actual.token());

        verify(orderRepository).findPageIds(specification, cursor.createdAt(), cursor.id(), 3);
        verify(orderRepository).findPage(userId, List.of(firstOrder.getId(), secondOrder.getId()));
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

        OrderPageData actual = orderService.getAll(request);

        assertTrue(actual.orders().isEmpty());
        assertNull(actual.token());

        verify(orderSpecificationBuilder).buildAllOrders(request);
        verify(orderRepository).findPageIds(specification, null, null, 21);
        verify(orderRepository, never()).findPage(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllShouldReturnSortedOrdersAndNextToken() {
        OrderScrollRequest request = orderScrollRequest(2, null, null, null, "cursor-1");
        CursorPayload cursor = new CursorPayload(datetime(), 77L);
        Specification<Order> specification = mock(Specification.class);

        Order firstOrder = order(101L, 1L);
        Order secondOrder = order(102L, 2L);

        when(cursorService.decode("cursor-1")).thenReturn(cursor);
        when(orderSpecificationBuilder.buildAllOrders(request)).thenReturn(specification);
        when(orderRepository.findPageIds(specification, cursor.createdAt(), cursor.id(), 3))
            .thenReturn(List.of(101L, 102L, 999L));
        when(orderRepository.findPage(List.of(101L, 102L)))
            .thenReturn(new ArrayList<>(List.of(secondOrder, firstOrder)));
        when(cursorService.encode(new CursorPayload(secondOrder.getCreatedAt(), secondOrder.getId())))
            .thenReturn("next-token");

        OrderPageData actual = orderService.getAll(request);

        assertEquals(List.of(firstOrder, secondOrder), actual.orders());
        assertEquals("next-token", actual.token());
    }

    @Test
    void addItemShouldAddItemToEditableOrderFlushAndReturnOrder() {
        Long userId = id();
        Long orderId = id();

        Order existingOrder = order(orderId, userId);
        Item existingItem = item(501L);
        OrderAddItemRequest request = orderAddItemRequest(existingItem.getId(), 3);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(existingItem.getId()))
            .thenReturn(Optional.of(existingItem));

        Order actual = orderService.addItem(userId, orderId, request);

        assertSame(existingOrder, actual);
        assertTrue(existingOrder.hasItem(existingItem));
        assertEquals(1, existingOrder.getOrderItems().size());
        assertEquals(3, existingOrder.getOrderItems().getFirst().getQuantity());
        assertEquals(
            existingItem.getPrice().multiply(BigDecimal.valueOf(3)),
            existingOrder.getTotalPrice()
        );

        verify(orderRepository).findDetailed(orderId, userId);
        verify(itemRepository).findById(existingItem.getId());
        verify(orderRepository).flush();
    }

    @Test
    void addItemShouldThrowWhenOrderIsNotEditable() {
        Long userId = id();
        Long orderId = id();

        Order paidOrder = paidOrder(orderId, userId);
        OrderAddItemRequest request = orderAddItemRequest(id(), 2);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(paidOrder));

        assertThrows(
            OrderStateConflictException.class,
            () -> orderService.addItem(userId, orderId, request)
        );

        verify(orderRepository).findDetailed(orderId, userId);
        verifyNoInteractions(itemRepository);
        verify(orderRepository, never()).flush();
    }

    @Test
    void addItemShouldThrowWhenItemNotFound() {
        Long userId = id();
        Long orderId = id();

        Order existingOrder = order(orderId, userId);
        OrderAddItemRequest request = orderAddItemRequest(999L, 2);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(
            ItemNotFoundException.class,
            () -> orderService.addItem(userId, orderId, request)
        );

        verify(orderRepository).findDetailed(orderId, userId);
        verify(itemRepository).findById(999L);
        verify(orderRepository, never()).flush();
    }

    @Test
    void removeItemShouldRemoveExistingItemFlushAndReturnOrder() {
        Long userId = id();
        Long orderId = id();

        Item existingItem = item(701L);
        Order existingOrder = orderWithItem(orderId, userId, existingItem, 2);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(existingItem.getId()))
            .thenReturn(Optional.of(existingItem));

        Order actual = orderService.removeItem(userId, orderId, existingItem.getId());

        assertSame(existingOrder, actual);
        assertFalse(existingOrder.hasItem(existingItem));
        assertTrue(existingOrder.getOrderItems().isEmpty());
        assertEquals(BigDecimal.ZERO, existingOrder.getTotalPrice());

        verify(orderRepository).findDetailed(orderId, userId);
        verify(itemRepository).findById(existingItem.getId());
        verify(orderRepository).flush();
    }

    @Test
    void changeQuantityShouldUpdateExistingOrderItemFlushAndReturnOrder() {
        Long userId = id();
        Long orderId = id();

        Item existingItem = item(801L);
        Order existingOrder = orderWithItem(orderId, userId, existingItem, 1);
        OrderChangeQuantityRequest request = orderChangeQuantityRequest(existingItem.getId(), 5);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));
        when(itemRepository.findById(existingItem.getId()))
            .thenReturn(Optional.of(existingItem));

        Order actual = orderService.changeQuantity(userId, orderId, request);

        assertSame(existingOrder, actual);
        assertEquals(1, existingOrder.getOrderItems().size());
        assertEquals(5, existingOrder.getOrderItems().getFirst().getQuantity());
        assertEquals(
            existingItem.getPrice().multiply(BigDecimal.valueOf(5)),
            existingOrder.getTotalPrice()
        );

        verify(orderRepository).findDetailed(orderId, userId);
        verify(itemRepository).findById(existingItem.getId());
        verify(orderRepository).flush();
    }

    @Test
    void updateStatusShouldMarkOrderPaidFlushAndReturnOrder() {
        Long userId = id();
        Long orderId = id();

        Item existingItem = item(901L);
        Order existingOrder = orderWithItem(orderId, userId, existingItem, 2);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));

        Order actual = orderService.updateStatus(userId, orderId, OrderStatus.PAID);

        assertSame(existingOrder, actual);
        assertTrue(existingOrder.isPaid());

        verify(orderRepository).findDetailed(orderId, userId);
        verify(orderRepository).flush();
    }

    @Test
    void updateStatusShouldThrowWhenPayingEmptyOrder() {
        Long userId = id();
        Long orderId = id();

        Order existingOrder = order(orderId, userId);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));

        assertThrows(
            EmptyOrderException.class,
            () -> orderService.updateStatus(userId, orderId, OrderStatus.PAID)
        );

        verify(orderRepository).findDetailed(orderId, userId);
        verify(orderRepository, never()).flush();
    }

    @Test
    void updateStatusShouldMarkPaidOrderCompletedFlushAndReturnOrder() {
        Long userId = id();
        Long orderId = id();

        Order existingOrder = paidOrder(orderId, userId);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));

        Order actual = orderService.updateStatus(userId, orderId, OrderStatus.COMPLETED);

        assertSame(existingOrder, actual);
        assertTrue(existingOrder.isCompleted());

        verify(orderRepository).findDetailed(orderId, userId);
        verify(orderRepository).flush();
    }

    @Test
    void updateStatusShouldMarkOrderCancelledFlushAndReturnOrder() {
        Long userId = id();
        Long orderId = id();

        Order existingOrder = order(orderId, userId);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));

        Order actual = orderService.updateStatus(userId, orderId, OrderStatus.CANCELLED);

        assertSame(existingOrder, actual);
        assertTrue(existingOrder.isCancelled());

        verify(orderRepository).findDetailed(orderId, userId);
        verify(orderRepository).flush();
    }

    @Test
    void updateStatusShouldThrowWhenStatusIsNew() {
        Long userId = id();
        Long orderId = id();

        Order existingOrder = order(orderId, userId);

        when(orderRepository.findDetailed(orderId, userId))
            .thenReturn(Optional.of(existingOrder));

        assertThrows(
            OrderStatusInvalidException.class,
            () -> orderService.updateStatus(userId, orderId, OrderStatus.NEW)
        );

        verify(orderRepository).findDetailed(orderId, userId);
        verify(orderRepository, never()).flush();
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
        verifyNoInteractions(itemRepository, cursorService, orderSpecificationBuilder);
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
        verifyNoInteractions(itemRepository, cursorService, orderSpecificationBuilder);
    }

    @Test
    void restoreShouldRestoreDeletedOrderFlushAndReturnOrder() {
        Long userId = id();
        Long orderId = id();

        Order deletedOrder = deletedOrder(orderId, userId);

        when(orderRepository.findDeleted(orderId, userId))
            .thenReturn(Optional.of(deletedOrder));

        Order actual = orderService.restore(userId, orderId);

        assertSame(deletedOrder, actual);
        assertFalse(deletedOrder.isDeleted());

        verify(orderRepository).findDeleted(orderId, userId);
        verify(orderRepository).flush();
    }

    @Test
    void restoreShouldThrowWhenDeletedOrderNotFound() {
        Long userId = id();
        Long orderId = id();

        when(orderRepository.findDeleted(orderId, userId))
            .thenReturn(Optional.empty());

        assertThrows(
            OrderNotFoundException.class,
            () -> orderService.restore(userId, orderId)
        );

        verify(orderRepository).findDeleted(orderId, userId);
        verify(orderRepository, never()).flush();
    }

}
