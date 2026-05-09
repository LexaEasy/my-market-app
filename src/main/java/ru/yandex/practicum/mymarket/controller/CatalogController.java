package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.yandex.practicum.mymarket.dto.CatalogPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
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
    public String getCatalog(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            Model model
    ) {
        CatalogPage catalogPage = itemService.findCatalog(search, sort, pageNumber, pageSize);
        fillModel(model, catalogPage);

        return "items";
    }

    @GetMapping("/items/{id}")
    public String getItem(@PathVariable long id, Model model) {
        ItemDto item = itemService.findById(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }

        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/items")
    public String updateCatalogItem(
            @RequestParam long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam CartAction action
    ) {
        cartService.updateItemCount(id, action);
        return redirectToCatalog(search, sort, pageNumber, pageSize);
    }

    @PostMapping("/items/{id}")
    public String updateItem(@PathVariable long id, @RequestParam CartAction action, Model model) {
        cartService.updateItemCount(id, action);
        return getItem(id, model);
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
