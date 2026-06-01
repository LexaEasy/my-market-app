package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CheckoutResult;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderPaymentResult;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

@Service
public class OrderService {

    private final CartItemRepository cartItemRepository;
    private final AppUserService appUserService;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentClientService paymentClientService;

    public OrderService(
            CartItemRepository cartItemRepository,
            AppUserService appUserService,
            ItemRepository itemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentClientService paymentClientService
    ) {
        this.cartItemRepository = cartItemRepository;
        this.appUserService = appUserService;
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentClientService = paymentClientService;
    }

    @Transactional
    public Mono<CheckoutResult> buy(String username) {
        return findUserId(username)
                .flatMap(userId -> cartItemRepository.findAllByUserIdOrderByItemIdAsc(userId)
                        .collectList()
                        .flatMap(cartItems -> {
                            if (cartItems.isEmpty()) {
                                return Mono.just(CheckoutResult.empty());
                            }
                            return calculateTotal(cartItems)
                                    .flatMap(total -> paymentClientService.pay(total))
                                    .flatMap(payment -> finishCheckout(userId, cartItems, payment));
                        }));
    }

    @Transactional(readOnly = true)
    public Mono<OrderDto> findById(String username, long id) {
        return findUserId(username)
                .flatMap(userId -> orderRepository.findByIdAndUserId(id, userId))
                .flatMap(this::toDto);
    }

    @Transactional(readOnly = true)
    public Flux<OrderDto> findAll(String username) {
        return findUserId(username)
                .flatMapMany(userId -> orderRepository.findAllByUserIdOrderByIdAsc(userId))
                .flatMap(this::toDto);
    }

    private Mono<CheckoutResult> finishCheckout(long userId, List<CartItem> cartItems, OrderPaymentResult payment) {
        if (!payment.success()) {
            return Mono.just(CheckoutResult.rejected(payment.message()));
        }
        return saveOrder(userId, cartItems).map(CheckoutResult::paid);
    }

    private Mono<Long> calculateTotal(List<CartItem> cartItems) {
        return Flux.fromIterable(cartItems)
                .flatMap(cartItem -> itemRepository.findById(cartItem.getItemId())
                        .map(item -> item.getPrice() * cartItem.getQuantity()))
                .reduce(0L, Long::sum);
    }

    private Mono<Long> saveOrder(long userId, List<CartItem> cartItems) {
        return orderRepository.save(Order.create(userId))
                .flatMap(savedOrder -> createOrderItems(savedOrder.getId(), cartItems)
                        .as(orderItemRepository::saveAll)
                        .then(cartItemRepository.deleteAll(cartItems))
                        .thenReturn(savedOrder.getId()));
    }

    private Mono<Long> findUserId(String username) {
        return appUserService.findOrCreateByUsername(username)
                .filter(user -> user.isEnabled())
                .map(user -> user.getId());
    }

    private Flux<OrderItem> createOrderItems(long orderId, List<CartItem> cartItems) {
        return Flux.fromIterable(cartItems)
                .flatMap(cartItem -> itemRepository.findById(cartItem.getItemId())
                        .map(item -> new OrderItem(
                                orderId,
                                item.getTitle(),
                                item.getPrice(),
                                cartItem.getQuantity()
                        )));
    }

    private Mono<OrderDto> toDto(Order order) {
        return orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId())
                .map(this::toItemDto)
                .collectList()
                .map(items -> toOrderDto(order, items));
    }

    private OrderDto toOrderDto(Order order, List<ItemDto> items) {
        long totalSum = items.stream()
                .mapToLong(item -> item.price() * item.count())
                .sum();

        return new OrderDto(order.getId(), items, totalSum);
    }

    private ItemDto toItemDto(OrderItem orderItem) {
        return new ItemDto(
                -1,
                orderItem.getTitle(),
                "",
                "",
                orderItem.getPrice(),
                orderItem.getQuantity()
        );
    }
}
