package ru.yandex.practicum.mymarket.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PaymentAvailability;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final AppUserService appUserService;
    private final ItemRepository itemRepository;
    private final PaymentClientService paymentClientService;

    public CartService(
            CartItemRepository cartItemRepository,
            AppUserService appUserService,
            ItemRepository itemRepository,
            PaymentClientService paymentClientService
    ) {
        this.cartItemRepository = cartItemRepository;
        this.appUserService = appUserService;
        this.itemRepository = itemRepository;
        this.paymentClientService = paymentClientService;
    }

    @Transactional(readOnly = true)
    public Mono<CartPage> findCart(String username) {
        return findUserId(username)
                .flatMap(userId -> cartItemRepository.findAllByUserIdOrderByItemIdAsc(userId)
                        .flatMap(this::toDto)
                        .collectList()
                        .flatMap(this::toCartPage));
    }

    @Transactional
    public Mono<Void> updateItemCount(String username, long itemId, CartAction action) {
        return findUserId(username)
                .flatMap(userId -> switch (action) {
                    case PLUS -> addItem(userId, itemId);
                    case MINUS -> removeOneItem(userId, itemId);
                    case DELETE -> deleteItem(userId, itemId);
                });
    }

    private Mono<Void> addItem(long userId, long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .switchIfEmpty(findItem(itemId).map(item -> new CartItem(userId, item.getId(), 0)))
                .flatMap(cartItem -> {
                    cartItem.increase();
                    return cartItemRepository.save(cartItem);
                })
                .then();
    }

    private Mono<Void> removeOneItem(long userId, long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .flatMap(cartItem -> {
                    cartItem.decrease();
                    if (cartItem.getQuantity() == 0) {
                        return cartItemRepository.delete(cartItem);
                    }
                    return cartItemRepository.save(cartItem).then();
                });
    }

    private Mono<Void> deleteItem(long userId, long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .flatMap(cartItemRepository::delete);
    }

    private Mono<Long> findUserId(String username) {
        if (username == null || username.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));
        }
        return appUserService.findOrCreateByUsername(username)
                .filter(user -> user.isEnabled())
                .map(user -> user.getId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled")));
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

    private Mono<CartPage> toCartPage(List<ItemDto> items) {
        long total = items.stream()
                .mapToLong(item -> item.price() * item.count())
                .sum();

        if (items.isEmpty()) {
            return Mono.just(new CartPage(items, total));
        }

        return paymentClientService.getBalance()
                .map(payment -> toCartPage(items, total, payment));
    }

    private CartPage toCartPage(List<ItemDto> items, long total, PaymentAvailability payment) {
        if (!payment.available()) {
            return new CartPage(items, total, false, payment.balance(), false, payment.message());
        }

        boolean purchaseAvailable = payment.balance() >= total;
        String message = purchaseAvailable ? null : "Недостаточно средств для оформления заказа";
        return new CartPage(items, total, true, payment.balance(), purchaseAvailable, message);
    }
}
