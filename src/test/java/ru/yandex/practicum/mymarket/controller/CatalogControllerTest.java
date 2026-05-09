package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.CatalogPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogControllerTest {

    private final ItemService itemService = mock(ItemService.class);
    private final CartService cartService = mock(CartService.class);
    private final CatalogController catalogController = new CatalogController(itemService, cartService);

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
        Model model = new ExtendedModelMap();
        when(itemService.findCatalog("товар", "ALPHA", 1, 5)).thenReturn(Mono.just(catalogPage));

        StepVerifier.create(catalogController.getCatalog("товар", "ALPHA", 1, 5, model))
                .expectNext("items")
                .verifyComplete();

        assertThat(model.getAttribute("items")).isEqualTo(catalogPage.items());
        assertThat(model.getAttribute("search")).isEqualTo("товар");
        assertThat(model.getAttribute("sort")).isEqualTo("ALPHA");
        assertThat(model.getAttribute("paging")).isEqualTo(catalogPage.paging());
        verify(itemService).findCatalog("товар", "ALPHA", 1, 5);
    }

    @Test
    void shouldRenderItemsCatalogPage() {
        CatalogPage catalogPage = new CatalogPage(
                List.of(List.of(
                        new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 0),
                        ItemDto.placeholder(),
                        ItemDto.placeholder()
                )),
                "",
                "NO",
                new Paging(5, 1, false, false)
        );
        Model model = new ExtendedModelMap();
        when(itemService.findCatalog(null, null, null, null)).thenReturn(Mono.just(catalogPage));

        StepVerifier.create(catalogController.getCatalog(null, null, null, null, model))
                .expectNext("items")
                .verifyComplete();

        assertThat(model.getAttribute("items")).isEqualTo(catalogPage.items());
        assertThat(model.getAttribute("search")).isEqualTo("");
        assertThat(model.getAttribute("sort")).isEqualTo("NO");
        assertThat(model.getAttribute("paging")).isEqualTo(catalogPage.paging());
        verify(itemService, times(1)).findCatalog(null, null, null, null);
    }

    @Test
    void shouldRenderItemPage() {
        ItemDto item = new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 0);
        Model model = new ExtendedModelMap();
        when(itemService.findById(1)).thenReturn(Mono.just(item));

        StepVerifier.create(catalogController.getItem(1, model))
                .expectNext("item")
                .verifyComplete();

        assertThat(model.getAttribute("item")).isEqualTo(item);
        verify(itemService).findById(1);
    }

    @Test
    void shouldReturnNotFoundWhenItemDoesNotExist() {
        when(itemService.findById(404)).thenReturn(Mono.empty());

        StepVerifier.create(catalogController.getItem(404, new ExtendedModelMap()))
                .expectError(ResponseStatusException.class)
                .verify();

        verify(itemService).findById(404);
    }

    @Test
    void shouldUpdateCatalogItemAndRedirectBackToCatalog() {
        when(cartService.updateItemCount(1, CartAction.PLUS)).thenReturn(Mono.empty());

        StepVerifier.create(catalogController.updateCatalogItem(1, "товар", "PRICE", 2, 10, CartAction.PLUS))
                .expectNext("redirect:/items?search=%D1%82%D0%BE%D0%B2%D0%B0%D1%80&sort=PRICE&pageNumber=2&pageSize=10")
                .verifyComplete();

        verify(cartService).updateItemCount(1, CartAction.PLUS);
    }

    @Test
    void shouldUpdateItemAndRenderItemPage() {
        ItemDto item = new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 1);
        Model model = new ExtendedModelMap();
        when(cartService.updateItemCount(1, CartAction.PLUS)).thenReturn(Mono.empty());
        when(itemService.findById(1)).thenReturn(Mono.just(item));

        StepVerifier.create(catalogController.updateItem(1, CartAction.PLUS, model))
                .expectNext("item")
                .verifyComplete();

        assertThat(model.getAttribute("item")).isEqualTo(item);
        verify(cartService).updateItemCount(1, CartAction.PLUS);
        verify(itemService).findById(1);
    }
}
