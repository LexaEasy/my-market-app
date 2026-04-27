package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/items")
    public String getCart(Model model) {
        CartPage cartPage = cartService.findCart();

        model.addAttribute("items", cartPage.items());
        model.addAttribute("total", cartPage.total());

        return "cart";
    }
}
