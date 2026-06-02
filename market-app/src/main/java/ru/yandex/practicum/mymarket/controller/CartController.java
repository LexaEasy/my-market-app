package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartPage;
import ru.yandex.practicum.mymarket.dto.CartItemForm;
import ru.yandex.practicum.mymarket.service.CartService;

import java.security.Principal;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/items")
    public Mono<String> getCart(
            @RequestParam(defaultValue = "false") boolean paymentError,
            Principal principal,
            Model model
    ) {
        model.addAttribute("paymentError", paymentError);
        return fillModel(principal, model).thenReturn("cart");
    }

    @PostMapping("/cart/items")
    public Mono<String> updateCartItem(@ModelAttribute CartItemForm form, Principal principal, Model model) {
        model.addAttribute("paymentError", false);
        return cartService.updateItemCount(username(principal), form.getId(), form.getAction())
                .then(fillModel(principal, model))
                .thenReturn("cart");
    }

    private Mono<Void> fillModel(Principal principal, Model model) {
        return cartService.findCart(username(principal))
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

    private String username(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
