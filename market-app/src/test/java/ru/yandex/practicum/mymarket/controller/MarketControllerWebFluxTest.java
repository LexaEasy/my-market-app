package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.dto.CatalogPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {
        CatalogController.class,
        CartController.class,
        OrderController.class
})
class MarketControllerWebFluxTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldRenderCatalogPage() {
        CatalogPage catalogPage = new CatalogPage(
                List.of(List.of(
                        new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 0),
                        ItemDto.placeholder(),
                        ItemDto.placeholder()
                )),
                "товар",
                "ALPHA",
                new Paging(5, 1, false, false)
        );
        when(itemService.findCatalog("товар", "ALPHA", 1, 5)).thenReturn(Mono.just(catalogPage));

        webTestClient.get()
                .uri("/?search={search}&sort={sort}&pageNumber={pageNumber}&pageSize={pageSize}",
                        "товар", "ALPHA", 1, 5)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Витрина магазина"))
                .value(containsString("Товар"))
                .value(containsString("Страница: 1"));

        verify(itemService).findCatalog("товар", "ALPHA", 1, 5);
    }

    @Test
    void shouldRenderItemPage() {
        ItemDto item = new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 0);
        when(itemService.findById(1)).thenReturn(Mono.just(item));

        webTestClient.get()
                .uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Товар"))
                .value(containsString("Описание"))
                .value(containsString("100 руб."));

        verify(itemService).findById(1);
    }

    @Test
    void shouldReturnNotFoundWhenItemDoesNotExist() {
        when(itemService.findById(404)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/items/404")
                .exchange()
                .expectStatus().isNotFound();

        verify(itemService).findById(404);
    }

    @Test
    void shouldUpdateCatalogItemAndRedirectBackToCatalog() {
        when(cartService.updateItemCount(1, CartAction.PLUS)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/items?id=1&search={search}&sort=PRICE&pageNumber=2&pageSize=10&action=PLUS", "товар")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .valueEquals("Location",
                        "/items?search=%D1%82%D0%BE%D0%B2%D0%B0%D1%80&sort=PRICE&pageNumber=2&pageSize=10");

        verify(cartService).updateItemCount(1, CartAction.PLUS);
    }

    @Test
    void shouldUpdateItemAndRenderItemPage() {
        ItemDto item = new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 1);
        when(cartService.updateItemCount(1, CartAction.PLUS)).thenReturn(Mono.empty());
        when(itemService.findById(1)).thenReturn(Mono.just(item));

        webTestClient.post()
                .uri("/items/1?action=PLUS")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Товар"))
                .value(containsString(">1</span>"));

        verify(cartService).updateItemCount(1, CartAction.PLUS);
        verify(itemService).findById(1);
    }

    @Test
    void shouldRenderCartPage() {
        CartPage cartPage = new CartPage(
                List.of(new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 2)),
                200
        );
        when(cartService.findCart()).thenReturn(Mono.just(cartPage));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Товар"))
                .value(containsString("Итого: 200 руб."));

        verify(cartService).findCart();
    }

    @Test
    void shouldUpdateCartItemAndRenderCartPage() {
        CartPage cartPage = new CartPage(List.of(), 0);
        when(cartService.updateItemCount(1, CartAction.DELETE)).thenReturn(Mono.empty());
        when(cartService.findCart()).thenReturn(Mono.just(cartPage));

        webTestClient.post()
                .uri("/cart/items?id=1&action=DELETE")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Витрина магазина"));

        verify(cartService).updateItemCount(1, CartAction.DELETE);
        verify(cartService).findCart();
    }

    @Test
    void shouldBuyCartAndRedirectToNewOrder() {
        when(orderService.buy()).thenReturn(Mono.just(10L));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/10?newOrder=true");

        verify(orderService).buy();
    }

    @Test
    void shouldRedirectToCartWhenCartIsEmpty() {
        when(orderService.buy()).thenReturn(Mono.just(-1L));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(orderService).buy();
    }

    @Test
    void shouldRenderOrdersPage() {
        List<OrderDto> orders = List.of(new OrderDto(
                10,
                List.of(new ItemDto(-1, "Товар", "", "", 100, 2)),
                200
        ));
        when(orderService.findAll()).thenReturn(Flux.fromIterable(orders));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Заказ №10"))
                .value(containsString("Сумма: 200 руб."));

        verify(orderService).findAll();
    }

    @Test
    void shouldRenderOrderPage() {
        OrderDto order = new OrderDto(
                10,
                List.of(new ItemDto(-1, "Товар", "", "", 100, 2)),
                200
        );
        when(orderService.findById(10)).thenReturn(Mono.just(order));

        webTestClient.get()
                .uri("/orders/10?newOrder=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(containsString("Поздравляем! Успешная покупка!"))
                .value(containsString("Заказ №10"))
                .value(containsString("Сумма: 200 руб."));

        verify(orderService).findById(10);
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() {
        when(orderService.findById(404)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/orders/404")
                .exchange()
                .expectStatus().isNotFound();

        verify(orderService).findById(404);
    }
}
