package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CartService.class)
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void shouldIncreaseItemCount() {
        Item item = itemRepository.save(new Item("Товар", "Описание", "images/item.jpg", 100));

        cartService.updateItemCount(item.getId(), CartAction.PLUS);
        cartService.updateItemCount(item.getId(), CartAction.PLUS);

        assertThat(cartItemRepository.findByItemId(item.getId()))
                .isPresent()
                .get()
                .extracting(cartItem -> cartItem.getQuantity())
                .isEqualTo(2);
    }

    @Test
    void shouldRemoveCartItemWhenCountBecomesZero() {
        Item item = itemRepository.save(new Item("Товар", "Описание", "images/item.jpg", 100));

        cartService.updateItemCount(item.getId(), CartAction.PLUS);
        cartService.updateItemCount(item.getId(), CartAction.MINUS);

        assertThat(cartItemRepository.findByItemId(item.getId())).isEmpty();
    }

    @Test
    void shouldReturnCartPageWithTotal() {
        Item item = itemRepository.save(new Item("Товар", "Описание", "images/item.jpg", 100));
        cartService.updateItemCount(item.getId(), CartAction.PLUS);
        cartService.updateItemCount(item.getId(), CartAction.PLUS);

        CartPage cartPage = cartService.findCart();

        assertThat(cartPage.total()).isEqualTo(200);
        assertThat(cartPage.items())
                .singleElement()
                .satisfies(cartItem -> {
                    assertThat(cartItem.id()).isEqualTo(item.getId());
                    assertThat(cartItem.count()).isEqualTo(2);
                    assertThat(cartItem.price()).isEqualTo(100);
                });
    }
}
