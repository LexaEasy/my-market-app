package ru.yandex.practicum.mymarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment-service")
public record PaymentClientProperties(String baseUrl) {
}
