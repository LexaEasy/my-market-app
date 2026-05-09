package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldBuyCartAndRedirectToNewOrder() throws Exception {
        when(orderService.buy()).thenReturn(10L);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/10?newOrder=true"));

        verify(orderService).buy();
    }

    @Test
    void shouldRedirectToCartWhenCartIsEmpty() throws Exception {
        when(orderService.buy()).thenReturn(-1L);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(orderService).buy();
    }

    @Test
    void shouldRenderOrderPage() throws Exception {
        OrderDto order = new OrderDto(
                10,
                List.of(new ItemDto(-1, "Товар", "", "", 100, 2)),
                200
        );
        when(orderService.findById(10)).thenReturn(order);

        mockMvc.perform(get("/orders/10")
                        .param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("newOrder", true));

        verify(orderService).findById(10);
    }

    @Test
    void shouldRenderOrdersPage() throws Exception {
        List<OrderDto> orders = List.of(new OrderDto(
                10,
                List.of(new ItemDto(-1, "Товар", "", "", 100, 2)),
                200
        ));
        when(orderService.findAll()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", orders));

        verify(orderService).findAll();
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.findById(404)).thenReturn(null);

        mockMvc.perform(get("/orders/404"))
                .andExpect(status().isNotFound());

        verify(orderService).findById(404);
    }
}
