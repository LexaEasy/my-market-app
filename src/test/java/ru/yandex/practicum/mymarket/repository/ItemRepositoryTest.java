package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void shouldFindSeededItem() {
        StepVerifier.create(itemRepository.findById(1L))
                .assertNext(savedItem -> {
                    assertThat(savedItem.getId()).isEqualTo(1L);
                    assertThat(savedItem.getTitle()).isEqualTo("Футбольный мяч");
                    assertThat(savedItem.getPrice()).isEqualTo(1490L);
                })
                .verifyComplete();
    }
}
