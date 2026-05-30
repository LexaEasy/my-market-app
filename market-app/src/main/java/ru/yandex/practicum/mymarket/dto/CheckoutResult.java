package ru.yandex.practicum.mymarket.dto;

public record CheckoutResult(long orderId, boolean success, boolean emptyCart, String message) {

    public static CheckoutResult paid(long orderId) {
        return new CheckoutResult(orderId, true, false, null);
    }

    public static CheckoutResult empty() {
        return new CheckoutResult(-1, false, true, null);
    }

    public static CheckoutResult rejected(String message) {
        return new CheckoutResult(-1, false, false, message);
    }
}
