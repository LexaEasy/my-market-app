package ru.yandex.practicum.mymarket.model;

public enum ItemSort {
    NO,
    ALPHA,
    PRICE;

    public static ItemSort from(String value) {
        if (value == null || value.isBlank()) {
            return NO;
        }

        try {
            return ItemSort.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return NO;
        }
    }
}
