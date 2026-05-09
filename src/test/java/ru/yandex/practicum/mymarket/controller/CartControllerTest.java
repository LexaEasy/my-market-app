package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartControllerTest {

    private final CartService cartService = mock(CartService.class);
    private final CartController cartController = new CartController(cartService);

    @Test
    void shouldRenderCartPage() {
        CartPage cartPage = new CartPage(
                List.of(new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 2)),
                200
        );
        Model model = new ExtendedModelMap();
        when(cartService.findCart()).thenReturn(Mono.just(cartPage));

        StepVerifier.create(cartController.getCart(model))
                .expectNext("cart")
                .verifyComplete();

        assertThat(model.getAttribute("items")).isEqualTo(cartPage.items());
        assertThat(model.getAttribute("total")).isEqualTo(200L);
        verify(cartService).findCart();
    }

    @Test
    void shouldUpdateCartItemAndRenderCartPage() {
        CartPage cartPage = new CartPage(List.of(), 0);
        Model model = new ExtendedModelMap();
        when(cartService.updateItemCount(1, CartAction.DELETE)).thenReturn(Mono.empty());
        when(cartService.findCart()).thenReturn(Mono.just(cartPage));

        StepVerifier.create(cartController.updateCartItem(1, CartAction.DELETE, model))
                .expectNext("cart")
                .verifyComplete();

        assertThat(model.getAttribute("items")).isEqualTo(cartPage.items());
        assertThat(model.getAttribute("total")).isEqualTo(0L);
        verify(cartService).updateItemCount(1, CartAction.DELETE);
        verify(cartService).findCart();
    }
}
