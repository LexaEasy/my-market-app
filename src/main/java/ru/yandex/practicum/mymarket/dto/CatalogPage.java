package ru.yandex.practicum.mymarket.dto;

import java.util.List;

public record CatalogPage(
        List<List<ItemDto>> items,
        String search,
        String sort,
        Paging paging
) {
}
