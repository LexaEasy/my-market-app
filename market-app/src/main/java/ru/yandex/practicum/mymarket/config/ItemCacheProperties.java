package ru.yandex.practicum.mymarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.items.cache")
public record ItemCacheProperties(Duration ttl) {
}
