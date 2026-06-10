package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.CartItem;

import java.util.Collection;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Mono<CartItem> findByUserIdAndItemId(long userId, long itemId);

    Flux<CartItem> findAllByUserIdAndItemIdIn(long userId, Collection<Long> itemIds);

    Flux<CartItem> findAllByUserIdOrderByItemIdAsc(long userId);
}
