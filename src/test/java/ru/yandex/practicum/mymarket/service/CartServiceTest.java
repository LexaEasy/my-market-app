package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(CartService.class)
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        StepVerifier.create(cartItemRepository.deleteAll())
                .verifyComplete();
    }

    @Test
    void shouldIncreaseItemCount() {
        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(item.getId(), CartAction.PLUS)
                                .then(cartService.updateItemCount(item.getId(), CartAction.PLUS))
                                .then(cartItemRepository.findByItemId(item.getId()))))
                .assertNext(cartItem -> assertThat(cartItem.getQuantity()).isEqualTo(2))
                .verifyComplete();
    }

    @Test
    void shouldRemoveCartItemWhenCountBecomesZero() {
        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(item.getId(), CartAction.PLUS)
                                .then(cartService.updateItemCount(item.getId(), CartAction.MINUS))
                                .then(cartItemRepository.findByItemId(item.getId()))))
                .verifyComplete();
    }

    @Test
    void shouldReturnCartPageWithTotal() {
        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(item.getId(), CartAction.PLUS)
                                .then(cartService.updateItemCount(item.getId(), CartAction.PLUS))
                                .then(cartService.findCart())))
                .assertNext(cartPage -> {
                    assertThat(cartPage.total()).isEqualTo(2980);
                    assertThat(cartPage.items())
                            .singleElement()
                            .satisfies(cartItem -> {
                                assertThat(cartItem.count()).isEqualTo(2);
                                assertThat(cartItem.price()).isEqualTo(1490);
                            });
                })
                .verifyComplete();
    }

    private Mono<Item> findSeededItem() {
        return itemRepository.findById(1L);
    }
}
