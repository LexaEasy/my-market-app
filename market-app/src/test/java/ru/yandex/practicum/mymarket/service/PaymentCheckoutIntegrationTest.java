package ru.yandex.practicum.mymarket.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerResponse;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentCheckoutIntegrationTest {

    private static final AtomicReference<PaymentScenario> paymentScenario = new AtomicReference<>();
    private static final AtomicInteger payRequests = new AtomicInteger();
    private static final AtomicReference<String> lastPayRequest = new AtomicReference<>();
    private static final DisposableServer paymentServer = HttpServer.create()
            .host("localhost")
            .port(0)
            .route(routes -> routes
                    .get("/payments/balance", (request, response) -> handleBalance(response))
                    .post("/payments/pay", (request, response) -> request.receive()
                            .aggregate()
                            .asString()
                            .flatMap(body -> handlePay(response, body))))
            .bindNow();

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

    @DynamicPropertySource
    static void paymentProperties(DynamicPropertyRegistry registry) {
        registry.add("app.payment-service.base-url", () -> "http://localhost:" + paymentServer.port());
    }

    @AfterAll
    static void stopPaymentServer() {
        paymentServer.disposeNow();
    }

    @BeforeEach
    void setUp() {
        paymentScenario.set(PaymentScenario.success());
        payRequests.set(0);
        lastPayRequest.set(null);
        StepVerifier.create(orderItemRepository.deleteAll()
                        .then(orderRepository.deleteAll())
                        .then(cartItemRepository.deleteAll()))
                .verifyComplete();
    }

    @Test
    void shouldCreateOrderAfterSuccessfulPayment() {
        StepVerifier.create(addSeededItemToCart()
                        .then(orderService.buy())
                        .flatMap(result -> Mono.zip(
                                Mono.just(result),
                                orderRepository.findById(result.orderId()),
                                cartItemRepository.findAll().collectList()
                        )))
                .assertNext(result -> {
                    assertThat(result.getT1().success()).isTrue();
                    assertThat(result.getT2().getId()).isEqualTo(result.getT1().orderId());
                    assertThat(result.getT3()).isEmpty();
                    assertThat(payRequests).hasValue(1);
                    assertThat(lastPayRequest.get()).contains("\"amount\":1490");
                })
                .verifyComplete();
    }

    @Test
    void shouldDisablePurchaseWhenBalanceIsNotEnough() {
        paymentScenario.set(PaymentScenario.notEnoughBalance());

        StepVerifier.create(addSeededItemToCart().then(cartService.findCart()))
                .assertNext(cartPage -> {
                    assertThat(cartPage.paymentAvailable()).isTrue();
                    assertThat(cartPage.purchaseAvailable()).isFalse();
                    assertThat(cartPage.paymentMessage()).isEqualTo("Недостаточно средств для оформления заказа");
                })
                .verifyComplete();
    }

    @Test
    void shouldNotCreateOrderWhenPaymentIsRejected() {
        paymentScenario.set(PaymentScenario.rejected());

        StepVerifier.create(addSeededItemToCart()
                        .then(orderService.buy())
                        .flatMap(result -> orderRepository.findAll()
                                .collectList()
                                .map(orders -> new RejectedCheckout(result.success(), result.message(), orders.size()))))
                .assertNext(result -> {
                    assertThat(result.success()).isFalse();
                    assertThat(result.message()).isEqualTo("Недостаточно средств");
                    assertThat(result.orderCount()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void shouldDisablePurchaseWhenPaymentServiceIsUnavailable() {
        paymentScenario.set(PaymentScenario.serviceUnavailable());

        StepVerifier.create(addSeededItemToCart().then(cartService.findCart()))
                .assertNext(cartPage -> {
                    assertThat(cartPage.paymentAvailable()).isFalse();
                    assertThat(cartPage.purchaseAvailable()).isFalse();
                    assertThat(cartPage.paymentMessage()).isEqualTo("Сервис платежей недоступен");
                })
                .verifyComplete();
    }

    private Mono<Void> addSeededItemToCart() {
        return itemRepository.findById(1L)
                .flatMap(item -> cartService.updateItemCount(item.getId(), CartAction.PLUS));
    }

    private static Mono<Void> handleBalance(HttpServerResponse response) {
        PaymentScenario scenario = paymentScenario.get();
        if (scenario.unavailable()) {
            return sendJson(response, HttpResponseStatus.SERVICE_UNAVAILABLE, "{\"message\":\"Service unavailable\"}");
        }
        return sendJson(response, HttpResponseStatus.OK, "{\"balance\":" + scenario.balance() + "}");
    }

    private static Mono<Void> handlePay(HttpServerResponse response, String body) {
        payRequests.incrementAndGet();
        lastPayRequest.set(body);
        PaymentScenario scenario = paymentScenario.get();
        return sendJson(response, scenario.payStatus(), scenario.payBody());
    }

    private static Mono<Void> sendJson(HttpServerResponse response, HttpResponseStatus status, String body) {
        response.status(status);
        response.header("Content-Type", "application/json");
        return response.sendString(Mono.just(body)).then();
    }

    private record PaymentScenario(long balance, HttpResponseStatus payStatus, String payBody, boolean unavailable) {

        static PaymentScenario success() {
            return new PaymentScenario(10000, HttpResponseStatus.OK, "{\"success\":true,\"balance\":8510}", false);
        }

        static PaymentScenario notEnoughBalance() {
            return new PaymentScenario(1000, HttpResponseStatus.OK, "{\"success\":true,\"balance\":1000}", false);
        }

        static PaymentScenario rejected() {
            return new PaymentScenario(
                    10000,
                    HttpResponseStatus.CONFLICT,
                    "{\"success\":false,\"balance\":1000,\"message\":\"Недостаточно средств\"}",
                    false
            );
        }

        static PaymentScenario serviceUnavailable() {
            return new PaymentScenario(0, HttpResponseStatus.SERVICE_UNAVAILABLE, "{\"message\":\"Service unavailable\"}", true);
        }
    }

    private record RejectedCheckout(boolean success, String message, int orderCount) {
    }
}
