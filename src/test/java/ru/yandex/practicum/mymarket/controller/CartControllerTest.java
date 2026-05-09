package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void shouldRenderCartPage() throws Exception {
        CartPage cartPage = new CartPage(
                List.of(new ItemDto(1, "Товар", "Описание", "images/item.jpg", 100, 2)),
                200
        );
        when(cartService.findCart()).thenReturn(cartPage);

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", cartPage.items()))
                .andExpect(model().attribute("total", 200L));

        verify(cartService).findCart();
    }

    @Test
    void shouldUpdateCartItemAndRenderCartPage() throws Exception {
        CartPage cartPage = new CartPage(List.of(), 0);
        when(cartService.findCart()).thenReturn(cartPage);

        mockMvc.perform(post("/cart/items")
                        .param("id", "1")
                        .param("action", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", cartPage.items()))
                .andExpect(model().attribute("total", 0L));

        verify(cartService).updateItemCount(1, CartAction.DELETE);
        verify(cartService).findCart();
    }
}
