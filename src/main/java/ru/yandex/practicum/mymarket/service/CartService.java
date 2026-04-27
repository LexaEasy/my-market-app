package ru.yandex.practicum.mymarket.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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
    public CartPage findCart() {
        List<ItemDto> items = cartItemRepository.findAllByOrderByItemIdAsc().stream()
                .map(this::toDto)
                .toList();
        long total = items.stream()
                .mapToLong(item -> item.price() * item.count())
                .sum();

        return new CartPage(items, total);
    }

    @Transactional
    public void updateItemCount(long itemId, CartAction action) {
        switch (action) {
            case PLUS -> addItem(itemId);
            case MINUS -> removeOneItem(itemId);
            case DELETE -> cartItemRepository.findByItemId(itemId).ifPresent(cartItemRepository::delete);
        }
    }

    private void addItem(long itemId) {
        CartItem cartItem = cartItemRepository.findByItemId(itemId)
                .orElseGet(() -> new CartItem(findItem(itemId), 0));
        cartItem.increase();
        cartItemRepository.save(cartItem);
    }

    private void removeOneItem(long itemId) {
        cartItemRepository.findByItemId(itemId).ifPresent(cartItem -> {
            cartItem.decrease();
            if (cartItem.getQuantity() == 0) {
                cartItemRepository.delete(cartItem);
            }
        });
    }

    private Item findItem(long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
    }

    private ItemDto toDto(CartItem cartItem) {
        Item item = cartItem.getItem();
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImgPath(),
                item.getPrice(),
                cartItem.getQuantity()
        );
    }
}
