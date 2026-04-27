package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({CartService.class, OrderService.class})
class OrderServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldCreateOrderAndClearCart() {
        Item item = itemRepository.save(new Item("Товар", "Описание", "images/item.jpg", 100));
        cartService.updateItemCount(item.getId(), CartAction.PLUS);
        cartService.updateItemCount(item.getId(), CartAction.PLUS);

        long orderId = orderService.buy();

        assertThat(orderId).isPositive();
        assertThat(cartItemRepository.findAll()).isEmpty();
        assertThat(orderRepository.findById(orderId))
                .isPresent()
                .get()
                .satisfies(order -> assertThat(order.getItems())
                        .singleElement()
                        .satisfies(orderItem -> {
                            assertThat(orderItem.getTitle()).isEqualTo("Товар");
                            assertThat(orderItem.getPrice()).isEqualTo(100);
                            assertThat(orderItem.getQuantity()).isEqualTo(2);
                        }));
    }

    @Test
    void shouldReturnMinusOneWhenCartIsEmpty() {
        long orderId = orderService.buy();

        assertThat(orderId).isEqualTo(-1);
        assertThat(orderRepository.findAll()).isEmpty();
    }
}
