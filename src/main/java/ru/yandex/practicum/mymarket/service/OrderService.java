package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

@Service
public class OrderService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;

    public OrderService(CartItemRepository cartItemRepository, OrderRepository orderRepository) {
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public long buy() {
        List<CartItem> cartItems = cartItemRepository.findAllByOrderByItemIdAsc();
        if (cartItems.isEmpty()) {
            return -1;
        }

        Order order = Order.create();
        cartItems.forEach(cartItem -> order.addItem(cartItem.getItem(), cartItem.getQuantity()));
        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);

        return savedOrder.getId();
    }

    @Transactional(readOnly = true)
    public OrderDto findById(long id) {
        return orderRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findAll() {
        return orderRepository.findAllByOrderByIdAsc().stream()
                .map(this::toDto)
                .toList();
    }

    private OrderDto toDto(Order order) {
        List<ItemDto> items = order.getItems().stream()
                .map(this::toItemDto)
                .toList();
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
