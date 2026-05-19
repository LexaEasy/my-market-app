package ru.yandex.practicum.mymarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.config.ItemCacheProperties;
import ru.yandex.practicum.mymarket.model.Item;

import java.util.List;

@Service
public class ItemCacheService {

    private static final String ALL_ITEMS_KEY = "items:all";
    private static final String ITEM_KEY_PREFIX = "items:card:";

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ItemCacheProperties properties;

    public ItemCacheService(
            ReactiveRedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            ItemCacheProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public Flux<Item> findAll(Flux<Item> databaseItems) {
        return redisTemplate.opsForValue()
                .get(ALL_ITEMS_KEY)
                .map(cached -> objectMapper.convertValue(cached, CachedItems.class))
                .flatMapMany(cached -> Flux.fromIterable(cached.items()).map(this::toItem))
                .switchIfEmpty(loadAll(databaseItems))
                .onErrorResume(error -> databaseItems);
    }

    public Mono<Item> findById(long id, Mono<Item> databaseItem) {
        return redisTemplate.opsForValue()
                .get(itemKey(id))
                .map(cached -> objectMapper.convertValue(cached, CachedItem.class))
                .map(this::toItem)
                .switchIfEmpty(loadOne(id, databaseItem))
                .onErrorResume(error -> databaseItem);
    }

    private Flux<Item> loadAll(Flux<Item> databaseItems) {
        return databaseItems.collectList()
                .flatMap(items -> redisTemplate.opsForValue()
                        .set(ALL_ITEMS_KEY, toCachedItems(items), properties.ttl())
                        .onErrorReturn(false)
                        .thenReturn(items))
                .flatMapMany(Flux::fromIterable);
    }

    private Mono<Item> loadOne(long id, Mono<Item> databaseItem) {
        return databaseItem.flatMap(item -> redisTemplate.opsForValue()
                .set(itemKey(id), toCachedItem(item), properties.ttl())
                .onErrorReturn(false)
                .thenReturn(item));
    }

    private CachedItems toCachedItems(List<Item> items) {
        return new CachedItems(items.stream()
                .map(this::toCachedItem)
                .toList());
    }

    private CachedItem toCachedItem(Item item) {
        return new CachedItem(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice()
        );
    }

    private Item toItem(CachedItem item) {
        return new Item(
                item.id(),
                item.title(),
                item.description(),
                item.imgPath(),
                item.price()
        );
    }

    private String itemKey(long id) {
        return ITEM_KEY_PREFIX + id;
    }

    public record CachedItems(List<CachedItem> items) {
    }

    public record CachedItem(Long id, String title, String description, String imgPath, long price) {
    }
}
