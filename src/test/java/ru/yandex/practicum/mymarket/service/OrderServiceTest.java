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
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
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

    @Autowired
    private OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        StepVerifier.create(orderItemRepository.deleteAll()
                        .then(orderRepository.deleteAll())
                        .then(cartItemRepository.deleteAll()))
                .verifyComplete();
    }

    @Test
    void shouldCreateOrderAndClearCart() {
        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(item.getId(), CartAction.PLUS)
                                .then(cartService.updateItemCount(item.getId(), CartAction.PLUS))
                                .then(orderService.buy()))
                        .flatMap(orderId -> Mono.zip(
                                Mono.just(orderId),
                                cartItemRepository.findAll().collectList(),
                                orderRepository.findById(orderId),
                                orderItemRepository.findAllByOrderIdOrderByIdAsc(orderId).collectList()
                        )))
                .assertNext(result -> {
                    assertThat(result.getT1()).isPositive();
                    assertThat(result.getT2()).isEmpty();
                    assertThat(result.getT3().getId()).isEqualTo(result.getT1());
                    assertThat(result.getT4())
                            .singleElement()
                            .satisfies(orderItem -> {
                                assertThat(orderItem.getTitle()).isEqualTo("Футбольный мяч");
                                assertThat(orderItem.getPrice()).isEqualTo(1490);
                                assertThat(orderItem.getQuantity()).isEqualTo(2);
                            });
                })
                .verifyComplete();
    }

    @Test
    void shouldFindOrderDtoById() {
        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(item.getId(), CartAction.PLUS)
                                .then(cartService.updateItemCount(item.getId(), CartAction.PLUS))
                                .then(orderService.buy()))
                        .flatMap(orderService::findById))
                .assertNext(order -> {
                    assertThat(order.totalSum()).isEqualTo(2980);
                    assertThat(order.items())
                            .singleElement()
                            .satisfies(orderItem -> {
                                assertThat(orderItem.title()).isEqualTo("Футбольный мяч");
                                assertThat(orderItem.price()).isEqualTo(1490);
                                assertThat(orderItem.count()).isEqualTo(2);
                            });
                })
                .verifyComplete();
    }

    @Test
    void shouldFindAllOrders() {
        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(item.getId(), CartAction.PLUS)
                                .then(orderService.buy()))
                        .flatMapMany(orderId -> orderService.findAll()))
                .assertNext(order -> {
                    assertThat(order.totalSum()).isEqualTo(1490);
                    assertThat(order.items()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnMinusOneWhenCartIsEmpty() {
        StepVerifier.create(orderService.buy()
                        .flatMap(orderId -> orderRepository.findAll()
                                .collectList()
                                .map(orders -> new EmptyCartResult(orderId, orders.size()))))
                .assertNext(result -> {
                    assertThat(result.orderId()).isEqualTo(-1);
                    assertThat(result.orderCount()).isZero();
                })
                .verifyComplete();
    }

    private Mono<Item> findSeededItem() {
        return itemRepository.findById(1L);
    }

    private record EmptyCartResult(long orderId, int orderCount) {
    }
}
