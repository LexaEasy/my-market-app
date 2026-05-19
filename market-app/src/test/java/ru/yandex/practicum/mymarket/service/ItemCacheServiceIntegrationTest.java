package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.Item;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ItemCacheServiceIntegrationTest {

    private static final int REDIS_PORT = 6379;

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(REDIS_PORT);

    @Autowired
    private ItemCacheService itemCacheService;

    @Autowired
    private ReactiveRedisConnectionFactory redisConnectionFactory;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(REDIS_PORT));
    }

    @BeforeEach
    void flushRedis() {
        redisConnectionFactory.getReactiveConnection()
                .serverCommands()
                .flushDb()
                .block();
    }

    @Test
    void shouldCacheItemCardAfterCacheMiss() {
        AtomicInteger databaseCalls = new AtomicInteger();
        Mono<Item> databaseItem = Mono.defer(() -> {
            databaseCalls.incrementAndGet();
            return Mono.just(new Item(10L, "Товар", "Описание", "images/item.jpg", 100));
        });

        StepVerifier.create(itemCacheService.findById(10L, databaseItem))
                .expectNextMatches(item -> item.getId() == 10L && item.getTitle().equals("Товар"))
                .verifyComplete();

        StepVerifier.create(itemCacheService.findById(10L, Mono.error(new AssertionError("Database should not be called"))))
                .expectNextMatches(item -> item.getId() == 10L && item.getTitle().equals("Товар"))
                .verifyComplete();

        StepVerifier.create(Mono.just(databaseCalls.get()))
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    void shouldCacheItemListAfterCacheMiss() {
        AtomicInteger databaseCalls = new AtomicInteger();
        List<Item> items = List.of(
                new Item(1L, "Первый товар", "Описание", "images/first.jpg", 100),
                new Item(2L, "Второй товар", "Описание", "images/second.jpg", 200)
        );
        Flux<Item> databaseItems = Flux.defer(() -> {
            databaseCalls.incrementAndGet();
            return Flux.fromIterable(items);
        });

        StepVerifier.create(itemCacheService.findAll(databaseItems).map(Item::getId))
                .expectNext(1L, 2L)
                .verifyComplete();

        StepVerifier.create(itemCacheService.findAll(Flux.error(new AssertionError("Database should not be called")))
                        .map(Item::getTitle))
                .expectNext("Первый товар", "Второй товар")
                .verifyComplete();

        StepVerifier.create(Mono.just(databaseCalls.get()))
                .expectNext(1)
                .verifyComplete();
    }
}
