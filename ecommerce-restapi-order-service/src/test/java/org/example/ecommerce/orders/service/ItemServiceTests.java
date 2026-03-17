package org.example.ecommerce.orders.service;

import org.example.ecommerce.orders.dto.request.CursorPayload;
import org.example.ecommerce.orders.dto.request.ItemCreateRequest;
import org.example.ecommerce.orders.dto.request.ItemScrollRequest;
import org.example.ecommerce.orders.dto.request.ItemUpdateRequest;
import org.example.ecommerce.orders.dto.response.ItemPageResponse;
import org.example.ecommerce.orders.dto.response.ItemResponse;
import org.example.ecommerce.orders.entity.Item;
import org.example.ecommerce.orders.exception.custom.item.ItemNotFoundException;
import org.example.ecommerce.orders.mapper.ItemMapper;
import org.example.ecommerce.orders.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.example.ecommerce.orders.support.TestDataGenerator.datetime;
import static org.example.ecommerce.orders.support.TestDataGenerator.id;
import static org.example.ecommerce.orders.support.TestDataGenerator.item;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemCreateRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemResponse;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemScrollRequest;
import static org.example.ecommerce.orders.support.TestDataGenerator.itemUpdateRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTests {

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CursorService cursorService;

    @InjectMocks
    private ItemService itemService;

    @Test
    void createShouldSaveMappedEntityAndReturnResponse() {
        ItemCreateRequest request = itemCreateRequest();
        Item toSave = item();
        Item saved = item();
        ItemResponse expected = itemResponse(saved);

        when(itemMapper.toEntity(request))
            .thenReturn(toSave);
        when(itemRepository.save(toSave))
            .thenReturn(saved);
        when(itemMapper.toResponse(saved))
            .thenReturn(expected);

        ItemResponse actual = itemService.create(request);

        assertEquals(expected, actual);
        verify(itemMapper).toEntity(request);
        verify(itemRepository).save(toSave);
        verify(itemMapper).toResponse(saved);
    }

    @Test
    void getShouldReturnItemResponse() {
        Long itemId = id();
        Item existingItem = item(itemId);
        ItemResponse expected = itemResponse(existingItem);

        when(itemRepository.findActive(itemId))
            .thenReturn(Optional.of(existingItem));
        when(itemMapper.toResponse(existingItem))
            .thenReturn(expected);

        ItemResponse actual = itemService.get(itemId);

        assertEquals(expected, actual);
        verify(itemRepository).findActive(itemId);
        verify(itemMapper).toResponse(existingItem);
    }

    @Test
    void getShouldThrowWhenItemNotFound() {
        Long itemId = id();

        when(itemRepository.findActive(itemId))
            .thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.get(itemId));
        verify(itemRepository).findActive(itemId);
        verifyNoInteractions(itemMapper);
    }

    @Test
    void getAllShouldUseDefaultPageSizeWhenSizeIsNull() {
        ItemScrollRequest request = itemScrollRequest(null, null);
        CursorPayload cursor = new CursorPayload(null, null);

        when(cursorService.decode(null))
            .thenReturn(cursor);
        when(itemRepository.findPage(null, null, 21))
            .thenReturn(List.of());

        ItemPageResponse actual = itemService.getAll(request);

        assertEquals(ItemPageResponse.empty(), actual);
        verify(cursorService).decode(null);
        verify(itemRepository).findPage(null, null, 21);
        verify(cursorService, never()).encode(any());
        verifyNoInteractions(itemMapper);
    }

    @Test
    void getAllShouldCapPageSizeAtMax() {
        ItemScrollRequest request = itemScrollRequest(999, "cursor");
        CursorPayload cursor = new CursorPayload(null, null);

        when(cursorService.decode("cursor"))
            .thenReturn(cursor);
        when(itemRepository.findPage(null, null, 51))
            .thenReturn(List.of());

        ItemPageResponse actual = itemService.getAll(request);

        assertEquals(ItemPageResponse.empty(), actual);
        verify(cursorService).decode("cursor");
        verify(itemRepository).findPage(null, null, 51);
        verify(cursorService, never()).encode(any());
        verifyNoInteractions(itemMapper);
    }

    @Test
    void getAllShouldReturnItemsAndNextTokenWhenHasNextPage() {
        ItemScrollRequest request = itemScrollRequest(2, "cursor-1");
        CursorPayload cursor = new CursorPayload(datetime(), 50L);

        Item first = item(101L);
        Item second = item(102L);
        Item extra = item(103L);

        ItemResponse firstResponse = itemResponse(first);
        ItemResponse secondResponse = itemResponse(second);

        when(cursorService.decode("cursor-1"))
            .thenReturn(cursor);
        when(itemRepository.findPage(cursor.createdAt(), cursor.id(), 3))
            .thenReturn(List.of(first, second, extra));
        when(itemMapper.toResponse(first))
            .thenReturn(firstResponse);
        when(itemMapper.toResponse(second))
            .thenReturn(secondResponse);
        when(cursorService.encode(new CursorPayload(second.getCreatedAt(), second.getId())))
            .thenReturn("next-token");

        ItemPageResponse actual = itemService.getAll(request);

        assertEquals(List.of(firstResponse, secondResponse), actual.items());
        assertEquals("next-token", actual.token());

        verify(cursorService).decode("cursor-1");
        verify(itemRepository).findPage(cursor.createdAt(), cursor.id(), 3);
        verify(itemMapper).toResponse(first);
        verify(itemMapper).toResponse(second);
        verify(itemMapper, never()).toResponse(extra);
        verify(cursorService).encode(new CursorPayload(second.getCreatedAt(), second.getId()));
    }

    @Test
    void updateShouldMutateExistingItemAndReturnResponse() {
        Long itemId = id();
        Item existingItem = item(itemId);
        ItemUpdateRequest request = itemUpdateRequest();
        ItemResponse expected = itemResponse(existingItem);

        when(itemRepository.findActive(itemId))
            .thenReturn(Optional.of(existingItem));
        when(itemMapper.toResponse(existingItem))
            .thenReturn(expected);

        ItemResponse actual = itemService.update(itemId, request);

        assertEquals(expected, actual);
        verify(itemRepository).findActive(itemId);
        verify(itemMapper).update(request, existingItem);
        verify(itemMapper).toResponse(existingItem);
    }

    @Test
    void updateShouldThrowWhenItemNotFound() {
        Long itemId = id();
        ItemUpdateRequest request = itemUpdateRequest();

        when(itemRepository.findActive(itemId))
            .thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.update(itemId, request));

        verify(itemRepository).findActive(itemId);
        verify(itemMapper, never()).update(any(), any());
        verify(itemMapper, never()).toResponse(any());
    }

    @Test
    void deleteShouldArchiveItem() {
        Long itemId = id();
        Item existingItem = item(itemId);

        when(itemRepository.findActive(itemId))
            .thenReturn(Optional.of(existingItem));

        itemService.delete(itemId);

        assertTrue(existingItem.getArchived());
        verify(itemRepository).findActive(itemId);
        verifyNoInteractions(itemMapper, cursorService);
    }

    @Test
    void deleteShouldThrowWhenItemNotFound() {
        Long itemId = id();

        when(itemRepository.findActive(itemId))
            .thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.delete(itemId));

        verify(itemRepository).findActive(itemId);
        verifyNoInteractions(itemMapper, cursorService);
    }

    @Test
    void restoreShouldRestoreArchivedItemFlushAndReturnResponse() {
        Long itemId = id();
        Item archivedItem = item(itemId);
        archivedItem.archive();

        ItemResponse expected = itemResponse(archivedItem);

        when(itemRepository.findArchived(itemId))
            .thenReturn(Optional.of(archivedItem));
        when(itemMapper.toResponse(archivedItem))
            .thenReturn(expected);

        ItemResponse actual = itemService.restore(itemId);

        assertEquals(expected, actual);
        assertFalse(archivedItem.getArchived());
        verify(itemRepository).findArchived(itemId);
        verify(itemRepository).flush();
        verify(itemMapper).toResponse(archivedItem);
    }

    @Test
    void restoreShouldThrowWhenArchivedItemNotFound() {
        Long itemId = id();

        when(itemRepository.findArchived(itemId))
            .thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.restore(itemId));

        verify(itemRepository).findArchived(itemId);
        verify(itemRepository, never()).flush();
        verifyNoInteractions(itemMapper);
    }

}
