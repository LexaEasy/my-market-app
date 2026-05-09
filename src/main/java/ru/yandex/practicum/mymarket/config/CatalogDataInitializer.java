package ru.yandex.practicum.mymarket.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;

@Configuration
public class CatalogDataInitializer {

    @Bean
    CommandLineRunner loadCatalog(ItemRepository itemRepository) {
        return args -> {
            if (itemRepository.count() > 0) {
                return;
            }

            itemRepository.saveAll(List.of(
                    new Item("Футбольный мяч", "Классический мяч для игры на открытом поле.", "images/ball.jpg", 1490),
                    new Item("Спортивная бутылка", "Лёгкая бутылка для тренировок и прогулок.", "images/bottle.jpg", 690),
                    new Item("Рюкзак городской", "Компактный рюкзак с отделением для ноутбука.", "images/backpack.jpg", 3490),
                    new Item("Кружка керамическая", "Кружка для горячих напитков объёмом 350 мл.", "images/mug.jpg", 540),
                    new Item("Настольная лампа", "Лампа с регулируемым углом наклона.", "images/lamp.jpg", 2190)
            ));
        };
    }
}
