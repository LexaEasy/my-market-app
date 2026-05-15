package ru.yandex.practicum.mymarket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CatalogPage;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
public class CatalogController {

    private final ItemService itemService;
    private final CartService cartService;

    public CatalogController(ItemService itemService, CartService cartService) {
        this.itemService = itemService;
        this.cartService = cartService;
    }

    @GetMapping({"/", "/items"})
    public Mono<String> getCatalog(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            Model model
    ) {
        return itemService.findCatalog(search, sort, pageNumber, pageSize)
                .doOnNext(catalogPage -> fillModel(model, catalogPage))
                .thenReturn("items");
    }

    @GetMapping("/items/{id}")
    public Mono<String> getItem(@PathVariable long id, Model model) {
        return itemService.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found")))
                .doOnNext(item -> model.addAttribute("item", item))
                .thenReturn("item");
    }

    @PostMapping("/items")
    public Mono<String> updateCatalogItem(
            @RequestParam long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam CartAction action
    ) {
        return cartService.updateItemCount(id, action)
                .thenReturn(redirectToCatalog(search, sort, pageNumber, pageSize));
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateItem(@PathVariable long id, @RequestParam CartAction action, Model model) {
        return cartService.updateItemCount(id, action)
                .then(getItem(id, model));
    }

    private void fillModel(Model model, CatalogPage catalogPage) {
        model.addAttribute("items", catalogPage.items());
        model.addAttribute("search", catalogPage.search());
        model.addAttribute("sort", catalogPage.sort());
        model.addAttribute("paging", catalogPage.paging());
    }

    private String redirectToCatalog(String search, String sort, Integer pageNumber, Integer pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/items");
        addQueryParam(builder, "search", search);
        addQueryParam(builder, "sort", sort);
        addQueryParam(builder, "pageNumber", pageNumber);
        addQueryParam(builder, "pageSize", pageSize);
        return "redirect:" + builder.toUriString();
    }

    private void addQueryParam(UriComponentsBuilder builder, String name, Object value) {
        if (value != null) {
            builder.queryParam(name, value);
        }
    }
}
