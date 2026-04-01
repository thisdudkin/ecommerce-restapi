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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItemService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final ItemMapper itemMapper;
    private final ItemRepository itemRepository;
    private final CursorService cursorService;

    public ItemService(ItemMapper itemMapper, ItemRepository itemRepository, CursorService cursorService) {
        this.itemMapper = itemMapper;
        this.itemRepository = itemRepository;
        this.cursorService = cursorService;
    }

    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        return itemMapper.toResponse(
            itemRepository.save(
                itemMapper.toEntity(request)
            )
        );
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long id) {
        return itemMapper.toResponse(getItem(id));
    }

    @Transactional(readOnly = true)
    public ItemPageResponse getAll(ItemScrollRequest request) {
        int pageSize = normalizeSize(request.size());
        CursorPayload cursor = cursorService.decode(request.token());

        List<Item> items = itemRepository.findPage(
            cursor.createdAt(),
            cursor.id(),
            pageSize + 1
        );

        boolean hasNext = items.size() > pageSize;
        if (hasNext)
            items = items.subList(0, pageSize);

        String nextToken = null;
        if (hasNext && !items.isEmpty()) {
            Item lastItem = items.getLast();
            nextToken = cursorService.encode(new CursorPayload(lastItem.getCreatedAt(), lastItem.getId()));
        }

        List<ItemResponse> responses = items.stream()
            .map(itemMapper::toResponse)
            .toList();

        return new ItemPageResponse(responses, nextToken);
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest request) {
        Item existing = getItem(id);
        itemMapper.update(request, existing);
        return itemMapper.toResponse(existing);
    }

    @Transactional
    public void delete(Long id) {
        Item item = getItem(id);
        item.archive();
    }

    @Transactional
    public ItemResponse restore(Long itemId) {
        Item archived = itemRepository.findArchived(itemId)
            .orElseThrow(() -> new ItemNotFoundException(itemId));

        archived.restore();
        itemRepository.flush();

        return itemMapper.toResponse(archived);
    }

    private Item getItem(Long itemId) {
        return itemRepository.findActive(itemId)
            .orElseThrow(() -> new ItemNotFoundException(itemId));
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 0)
            return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

}
