package ru.yandex.practicum.mymarket.dto;

public record OrderPaymentResult(boolean success, boolean serviceAvailable, long balance, String message) {

    public static OrderPaymentResult success(long balance) {
        return new OrderPaymentResult(true, true, balance, null);
    }

    public static OrderPaymentResult rejected(long balance, String message) {
        return new OrderPaymentResult(false, true, balance, message);
    }

    public static OrderPaymentResult unavailable(String message) {
        return new OrderPaymentResult(false, false, 0, message);
    }
}
