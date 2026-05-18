package ru.yandex.practicum.mymarket.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartService(CartItemRepository cartItemRepository, ItemRepository itemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public Mono<CartPage> findCart() {
        return cartItemRepository.findAllByOrderByItemIdAsc()
                .flatMap(this::toDto)
                .collectList()
                .map(this::toCartPage);
    }

    @Transactional
    public Mono<Void> updateItemCount(long itemId, CartAction action) {
        return switch (action) {
            case PLUS -> addItem(itemId);
            case MINUS -> removeOneItem(itemId);
            case DELETE -> deleteItem(itemId);
        };
    }

    private Mono<Void> addItem(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .switchIfEmpty(findItem(itemId).map(item -> new CartItem(item.getId(), 0)))
                .flatMap(cartItem -> {
                    cartItem.increase();
                    return cartItemRepository.save(cartItem);
                })
                .then();
    }

    private Mono<Void> removeOneItem(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .flatMap(cartItem -> {
                    cartItem.decrease();
                    if (cartItem.getQuantity() == 0) {
                        return cartItemRepository.delete(cartItem);
                    }
                    return cartItemRepository.save(cartItem).then();
                });
    }

    private Mono<Void> deleteItem(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .flatMap(cartItemRepository::delete);
    }

    private Mono<Item> findItem(long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found")));
    }

    private Mono<ItemDto> toDto(CartItem cartItem) {
        return findItem(cartItem.getItemId())
                .map(item -> new ItemDto(
                        item.getId(),
                        item.getTitle(),
                        item.getDescription(),
                        item.getImgPath(),
                        item.getPrice(),
                        cartItem.getQuantity()
                ));
    }

    private CartPage toCartPage(List<ItemDto> items) {
        long total = items.stream()
                .mapToLong(item -> item.price() * item.count())
                .sum();

        return new CartPage(items, total);
    }
}
