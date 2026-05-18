package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
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
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(
            CartItemRepository cartItemRepository,
            ItemRepository itemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public Mono<Long> buy() {
        return cartItemRepository.findAllByOrderByItemIdAsc()
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.just(-1L);
                    }
                    return saveOrder(cartItems);
                });
    }

    @Transactional(readOnly = true)
    public Mono<OrderDto> findById(long id) {
        return orderRepository.findById(id)
                .flatMap(this::toDto);
    }

    @Transactional(readOnly = true)
    public Flux<OrderDto> findAll() {
        return orderRepository.findAllByOrderByIdAsc()
                .flatMap(this::toDto);
    }

    private Mono<Long> saveOrder(List<CartItem> cartItems) {
        return orderRepository.save(Order.create())
                .flatMap(savedOrder -> createOrderItems(savedOrder.getId(), cartItems)
                        .as(orderItemRepository::saveAll)
                        .then(cartItemRepository.deleteAll(cartItems))
                        .thenReturn(savedOrder.getId()));
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
