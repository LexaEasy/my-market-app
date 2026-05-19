package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.dto.CartItemForm;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/items")
    public Mono<String> getCart(Model model) {
        return fillModel(model).thenReturn("cart");
    }

    @PostMapping("/cart/items")
    public Mono<String> updateCartItem(@ModelAttribute CartItemForm form, Model model) {
        return cartService.updateItemCount(form.getId(), form.getAction())
                .then(fillModel(model))
                .thenReturn("cart");
    }

    private Mono<Void> fillModel(Model model) {
        return cartService.findCart()
                .doOnNext(cartPage -> {
                    model.addAttribute("items", cartPage.items());
                    model.addAttribute("total", cartPage.total());
                    model.addAttribute("paymentAvailable", cartPage.paymentAvailable());
                    model.addAttribute("balance", cartPage.balance());
                    model.addAttribute("purchaseAvailable", cartPage.purchaseAvailable());
                    model.addAttribute("paymentMessage", cartPage.paymentMessage());
                })
                .then();
    }
}
