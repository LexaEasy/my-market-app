package ru.yandex.practicum.mymarket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.CatalogPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.ItemSort;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int ITEMS_PER_ROW = 3;

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    public ItemService(ItemRepository itemRepository, CartItemRepository cartItemRepository) {
        this.itemRepository = itemRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional(readOnly = true)
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ItemDto findById(long id) {
        return itemRepository.findById(id)
                .map(item -> toDto(item, findCount(item.getId())))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CatalogPage findCatalog(String search, String sort, Integer pageNumber, Integer pageSize) {
        String normalizedSearch = normalizeSearch(search);
        ItemSort itemSort = ItemSort.from(sort);
        int normalizedPageNumber = normalizePageNumber(pageNumber);
        int normalizedPageSize = normalizePageSize(pageSize);
        Pageable pageable = PageRequest.of(
                normalizedPageNumber - 1,
                normalizedPageSize,
                resolveSort(itemSort)
        );

        Page<Item> page = normalizedSearch.isBlank()
                ? itemRepository.findAll(pageable)
                : itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                normalizedSearch,
                normalizedSearch,
                pageable
        );

        List<Item> items = page.getContent();
        Map<Long, Integer> counts = findCounts(items);

        return new CatalogPage(
                toRows(items, counts),
                normalizedSearch,
                itemSort.name(),
                new Paging(normalizedPageSize, normalizedPageNumber, page.hasPrevious(), page.hasNext())
        );
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }

    private int normalizePageNumber(Integer pageNumber) {
        if (pageNumber == null || pageNumber < DEFAULT_PAGE_NUMBER) {
            return DEFAULT_PAGE_NUMBER;
        }
        return pageNumber;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }

    private Sort resolveSort(ItemSort sort) {
        return switch (sort) {
            case ALPHA -> Sort.by(Sort.Direction.ASC, "title");
            case PRICE -> Sort.by(Sort.Direction.ASC, "price");
            case NO -> Sort.unsorted();
        };
    }

    private List<List<ItemDto>> toRows(List<Item> items, Map<Long, Integer> counts) {
        List<List<ItemDto>> rows = new ArrayList<>();
        for (int index = 0; index < items.size(); index += ITEMS_PER_ROW) {
            List<ItemDto> row = new ArrayList<>();
            items.subList(index, Math.min(index + ITEMS_PER_ROW, items.size()))
                    .stream()
                    .map(item -> toDto(item, counts.getOrDefault(item.getId(), 0)))
                    .forEach(row::add);
            while (row.size() < ITEMS_PER_ROW) {
                row.add(ItemDto.placeholder());
            }
            rows.add(row);
        }
        return rows;
    }

    private Map<Long, Integer> findCounts(List<Item> items) {
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }

        return cartItemRepository.findAllByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(cartItem -> cartItem.getItem().getId(), cartItem -> cartItem.getQuantity()));
    }

    private int findCount(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .map(cartItem -> cartItem.getQuantity())
                .orElse(0);
    }

    private ItemDto toDto(Item item, int count) {
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                count
        );
    }
}
