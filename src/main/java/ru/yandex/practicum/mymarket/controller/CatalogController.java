package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.dto.CatalogPage;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
public class CatalogController {

    private final ItemService itemService;

    public CatalogController(ItemService itemService) {
        this.itemService = itemService;
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

    private void fillModel(Model model, CatalogPage catalogPage) {
        model.addAttribute("items", catalogPage.items());
        model.addAttribute("search", catalogPage.search());
        model.addAttribute("sort", catalogPage.sort());
        model.addAttribute("paging", catalogPage.paging());
    }
}
