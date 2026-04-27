package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/buy")
    public String buy() {
        long orderId = orderService.buy();
        if (orderId == -1) {
            return "redirect:/cart/items";
        }

        return "redirect:/orders/" + orderId + "?newOrder=true";
    }
}
