package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.yandex.practicum.mymarket.model.Item;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void shouldSaveAndFindItem() {
        Item item = new Item("Тестовый товар", "Описание товара", "images/test.jpg", 1000);

        Item savedItem = itemRepository.save(item);

        assertThat(savedItem.getId()).isNotNull();
        assertThat(itemRepository.findById(savedItem.getId()))
                .isPresent()
                .get()
                .extracting(Item::getTitle, Item::getPrice)
                .containsExactly("Тестовый товар", 1000L);
    }
}
