package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/items")
    public String getCart(Model model) {
        fillModel(model);
        return "cart";
    }

    @PostMapping("/cart/items")
    public String updateCartItem(@RequestParam long id, @RequestParam CartAction action, Model model) {
        cartService.updateItemCount(id, action);
        fillModel(model);
        return "cart";
    }

    private void fillModel(Model model) {
        CartPage cartPage = cartService.findCart();

        model.addAttribute("items", cartPage.items());
        model.addAttribute("total", cartPage.total());
    }
}
