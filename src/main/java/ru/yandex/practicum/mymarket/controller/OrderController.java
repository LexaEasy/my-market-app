package ru.yandex.practicum.mymarket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

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

    @GetMapping("/orders")
    public String getOrders(Model model) {
        List<OrderDto> orders = orderService.findAll();

        model.addAttribute("orders", orders);

        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(
            @PathVariable long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            Model model
    ) {
        OrderDto order = orderService.findById(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);

        return "order";
    }
}
