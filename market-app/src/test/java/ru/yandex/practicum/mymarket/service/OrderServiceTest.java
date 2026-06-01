package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.OrderPaymentResult;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataR2dbcTest
@Import({AppUserService.class, CartService.class, OrderService.class})
class OrderServiceTest {

    private static final String USERNAME = "user";
    private static final String OTHER_USERNAME = "buyer";

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

    @MockitoBean
    private PaymentClientService paymentClientService;

    @BeforeEach
    void setUp() {
        StepVerifier.create(orderItemRepository.deleteAll()
                        .then(orderRepository.deleteAll())
                        .then(cartItemRepository.deleteAll()))
                .verifyComplete();
    }

    @Test
    void shouldCreateOrderAndClearCart() {
        when(paymentClientService.pay(2980L)).thenReturn(Mono.just(OrderPaymentResult.success(7020L)));

        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(USERNAME, item.getId(), CartAction.PLUS)
                                .then(cartService.updateItemCount(USERNAME, item.getId(), CartAction.PLUS))
                                .then(orderService.buy(USERNAME)))
                        .flatMap(result -> Mono.zip(
                                Mono.just(result.orderId()),
                                cartItemRepository.findAll().collectList(),
                                orderRepository.findById(result.orderId()),
                                orderItemRepository.findAllByOrderIdOrderByIdAsc(result.orderId()).collectList()
                        )))
                .assertNext(result -> {
                    assertThat(result.getT1()).isPositive();
                    assertThat(result.getT2()).isEmpty();
                    assertThat(result.getT3().getId()).isEqualTo(result.getT1());
                    assertThat(result.getT3().getUserId()).isEqualTo(1L);
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
        when(paymentClientService.pay(2980L)).thenReturn(Mono.just(OrderPaymentResult.success(7020L)));

        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(USERNAME, item.getId(), CartAction.PLUS)
                                .then(cartService.updateItemCount(USERNAME, item.getId(), CartAction.PLUS))
                                .then(orderService.buy(USERNAME)))
                        .flatMap(result -> orderService.findById(USERNAME, result.orderId())))
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
        when(paymentClientService.pay(1490L)).thenReturn(Mono.just(OrderPaymentResult.success(8510L)));

        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(USERNAME, item.getId(), CartAction.PLUS)
                                .then(orderService.buy(USERNAME)))
                        .flatMapMany(result -> orderService.findAll(USERNAME)))
                .assertNext(order -> {
                    assertThat(order.totalSum()).isEqualTo(1490);
                    assertThat(order.items()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnMinusOneWhenCartIsEmpty() {
        StepVerifier.create(orderService.buy(USERNAME)
                        .flatMap(orderId -> orderRepository.findAll()
                                .collectList()
                                .map(orders -> new EmptyCartResult(orderId.orderId(), orders.size()))))
                .assertNext(result -> {
                    assertThat(result.orderId()).isEqualTo(-1);
                    assertThat(result.orderCount()).isZero();
                })
                .verifyComplete();

        verify(paymentClientService, never()).pay(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldNotCreateOrderWhenPaymentIsRejected() {
        when(paymentClientService.pay(1490L))
                .thenReturn(Mono.just(OrderPaymentResult.rejected(1000L, "Недостаточно средств")));

        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(USERNAME, item.getId(), CartAction.PLUS)
                                .then(orderService.buy(USERNAME)))
                        .flatMap(result -> orderRepository.findAll()
                                .collectList()
                                .map(orders -> new RejectedPaymentResult(result.success(), result.message(), orders.size()))))
                .assertNext(result -> {
                    assertThat(result.success()).isFalse();
                    assertThat(result.message()).isEqualTo("Недостаточно средств");
                    assertThat(result.orderCount()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void shouldKeepOrdersSeparatedByUser() {
        when(paymentClientService.pay(1490L)).thenReturn(Mono.just(OrderPaymentResult.success(8510L)));

        StepVerifier.create(findSeededItem()
                        .flatMap(item -> cartService.updateItemCount(USERNAME, item.getId(), CartAction.PLUS)
                                .then(orderService.buy(USERNAME))
                                .flatMap(firstOrder -> cartService.updateItemCount(OTHER_USERNAME, item.getId(), CartAction.PLUS)
                                        .then(orderService.buy(OTHER_USERNAME))
                                        .map(secondOrder -> new UserOrders(firstOrder.orderId(), secondOrder.orderId()))))
                        .flatMap(orders -> orderService.findById(OTHER_USERNAME, orders.firstOrderId())
                                .hasElement()
                                .zipWith(orderService.findAll(USERNAME).collectList())
                                .map(result -> new OrderIsolation(result.getT1(), result.getT2().size()))))
                .assertNext(result -> {
                    assertThat(result.otherUserCanReadFirstOrder()).isFalse();
                    assertThat(result.firstUserOrderCount()).isEqualTo(1);
                })
                .verifyComplete();
    }

    private Mono<Item> findSeededItem() {
        return itemRepository.findById(1L);
    }

    private record EmptyCartResult(long orderId, int orderCount) {
    }

    private record RejectedPaymentResult(boolean success, String message, int orderCount) {
    }

    private record UserOrders(long firstOrderId, long secondOrderId) {
    }

    private record OrderIsolation(boolean otherUserCanReadFirstOrder, int firstUserOrderCount) {
    }
}
