package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.dto.CatalogPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @Test
    void shouldRenderCatalogPage() throws Exception {
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
        when(itemService.findCatalog("товар", "ALPHA", 1, 5)).thenReturn(catalogPage);

        mockMvc.perform(get("/")
                        .param("search", "товар")
                        .param("sort", "ALPHA")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("items", catalogPage.items()))
                .andExpect(model().attribute("search", "товар"))
                .andExpect(model().attribute("sort", "ALPHA"))
                .andExpect(model().attribute("paging", catalogPage.paging()));

        verify(itemService).findCatalog("товар", "ALPHA", 1, 5);
    }

    @Test
    void shouldRenderItemsCatalogPage() throws Exception {
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
        when(itemService.findCatalog(null, null, null, null)).thenReturn(catalogPage);

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("items", catalogPage.items()))
                .andExpect(model().attribute("search", ""))
                .andExpect(model().attribute("sort", "NO"))
                .andExpect(model().attribute("paging", catalogPage.paging()));

        verify(itemService, times(1)).findCatalog(null, null, null, null);
    }

    @Test
    void shouldRenderItemPage() throws Exception {
        ItemDto item = new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 0);
        when(itemService.findById(1)).thenReturn(item);

        mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attribute("item", item));

        verify(itemService).findById(1);
    }

    @Test
    void shouldReturnNotFoundWhenItemDoesNotExist() throws Exception {
        when(itemService.findById(404)).thenReturn(null);

        mockMvc.perform(get("/items/404"))
                .andExpect(status().isNotFound());

        verify(itemService).findById(404);
    }

    @Test
    void shouldUpdateCatalogItemAndRedirectBackToCatalog() throws Exception {
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("search", "товар")
                        .param("sort", "PRICE")
                        .param("pageNumber", "2")
                .param("pageSize", "10")
                .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=%D1%82%D0%BE%D0%B2%D0%B0%D1%80&sort=PRICE&pageNumber=2&pageSize=10"));

        verify(cartService).updateItemCount(1, CartAction.PLUS);
    }

    @Test
    void shouldUpdateItemAndRenderItemPage() throws Exception {
        ItemDto item = new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 1);
        when(itemService.findById(1)).thenReturn(item);

        mockMvc.perform(post("/items/1")
                        .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attribute("item", item));

        verify(cartService).updateItemCount(1, CartAction.PLUS);
        verify(itemService).findById(1);
    }
}
