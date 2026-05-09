package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderControllerTest {

    private final OrderService orderService = mock(OrderService.class);
    private final OrderController orderController = new OrderController(orderService);

    @Test
    void shouldBuyCartAndRedirectToNewOrder() {
        when(orderService.buy()).thenReturn(Mono.just(10L));

        StepVerifier.create(orderController.buy())
                .expectNext("redirect:/orders/10?newOrder=true")
                .verifyComplete();

        verify(orderService).buy();
    }

    @Test
    void shouldRedirectToCartWhenCartIsEmpty() {
        when(orderService.buy()).thenReturn(Mono.just(-1L));

        StepVerifier.create(orderController.buy())
                .expectNext("redirect:/cart/items")
                .verifyComplete();

        verify(orderService).buy();
    }

    @Test
    void shouldRenderOrderPage() {
        OrderDto order = new OrderDto(
                10,
                List.of(new ItemDto(-1, "Товар", "", "", 100, 2)),
                200
        );
        Model model = new ExtendedModelMap();
        when(orderService.findById(10)).thenReturn(Mono.just(order));

        StepVerifier.create(orderController.getOrder(10, true, model))
                .expectNext("order")
                .verifyComplete();

        assertThat(model.getAttribute("order")).isEqualTo(order);
        assertThat(model.getAttribute("newOrder")).isEqualTo(true);
        verify(orderService).findById(10);
    }

    @Test
    void shouldRenderOrdersPage() {
        List<OrderDto> orders = List.of(new OrderDto(
                10,
                List.of(new ItemDto(-1, "Товар", "", "", 100, 2)),
                200
        ));
        Model model = new ExtendedModelMap();
        when(orderService.findAll()).thenReturn(Flux.fromIterable(orders));

        StepVerifier.create(orderController.getOrders(model))
                .expectNext("orders")
                .verifyComplete();

        assertThat(model.getAttribute("orders")).isEqualTo(orders);
        verify(orderService).findAll();
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() {
        when(orderService.findById(404)).thenReturn(Mono.empty());

        StepVerifier.create(orderController.getOrder(404, false, new ExtendedModelMap()))
                .expectError(ResponseStatusException.class)
                .verify();

        verify(orderService).findById(404);
    }
}
