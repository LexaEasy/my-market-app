package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CatalogPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.ItemSort;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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
    public Flux<Item> findAll() {
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Mono<ItemDto> findById(long id) {
        return itemRepository.findById(id)
                .flatMap(item -> findCount(item.getId()).map(count -> toDto(item, count)));
    }

    @Transactional(readOnly = true)
    public Mono<CatalogPage> findCatalog(String search, String sort, Integer pageNumber, Integer pageSize) {
        String normalizedSearch = normalizeSearch(search);
        ItemSort itemSort = ItemSort.from(sort);
        int normalizedPageNumber = normalizePageNumber(pageNumber);
        int normalizedPageSize = normalizePageSize(pageSize);

        return itemRepository.findAll()
                .filter(matchesSearch(normalizedSearch))
                .sort(resolveComparator(itemSort))
                .collectList()
                .flatMap(items -> buildCatalogPage(
                        items,
                        normalizedSearch,
                        itemSort,
                        normalizedPageNumber,
                        normalizedPageSize
                ));
    }

    private Mono<CatalogPage> buildCatalogPage(
            List<Item> allItems,
            String search,
            ItemSort sort,
            int pageNumber,
            int pageSize
    ) {
        int fromIndex = Math.min((pageNumber - 1) * pageSize, allItems.size());
        int toIndex = Math.min(fromIndex + pageSize, allItems.size());
        List<Item> pageItems = allItems.subList(fromIndex, toIndex);
        boolean hasPrevious = pageNumber > 1;
        boolean hasNext = toIndex < allItems.size();

        return findCounts(pageItems)
                .map(counts -> new CatalogPage(
                        toRows(pageItems, counts),
                        search,
                        sort.name(),
                        new Paging(pageSize, pageNumber, hasPrevious, hasNext)
                ));
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

    private Predicate<Item> matchesSearch(String search) {
        if (search.isBlank()) {
            return item -> true;
        }

        String lowerCaseSearch = search.toLowerCase();
        return item -> item.getTitle().toLowerCase().contains(lowerCaseSearch)
                || item.getDescription().toLowerCase().contains(lowerCaseSearch);
    }

    private Comparator<Item> resolveComparator(ItemSort sort) {
        return switch (sort) {
            case ALPHA -> Comparator.comparing(Item::getTitle, String.CASE_INSENSITIVE_ORDER);
            case PRICE -> Comparator.comparingLong(Item::getPrice);
            case NO -> Comparator.comparing(Item::getId);
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

    private Mono<Map<Long, Integer>> findCounts(List<Item> items) {
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();
        if (itemIds.isEmpty()) {
            return Mono.just(Map.of());
        }

        return cartItemRepository.findAllByItemIdIn(itemIds)
                .collect(Collectors.toMap(CartItem::getItemId, CartItem::getQuantity));
    }

    private Mono<Integer> findCount(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .map(cartItem -> cartItem.getQuantity())
                .defaultIfEmpty(0);
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
