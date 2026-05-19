package ru.yandex.practicum.mymarket.dto;

import java.util.List;

public record CartPage(
        List<ItemDto> items,
        long total,
        boolean paymentAvailable,
        long balance,
        boolean purchaseAvailable,
        String paymentMessage
) {

    public CartPage(List<ItemDto> items, long total) {
        this(items, total, true, 0, false, null);
    }
}
