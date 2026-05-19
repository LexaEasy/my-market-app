package ru.yandex.practicum.mymarket.dto;

public record PaymentAvailability(boolean available, long balance, String message) {

    public static PaymentAvailability available(long balance) {
        return new PaymentAvailability(true, balance, null);
    }

    public static PaymentAvailability unavailable(String message) {
        return new PaymentAvailability(false, 0, message);
    }
}
