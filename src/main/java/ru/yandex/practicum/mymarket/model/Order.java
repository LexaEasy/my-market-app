package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;

@Table("orders")
public class Order {

    @Id
    private Long id;

    @Transient
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public static Order create() {
        return new Order();
    }

    public Long getId() {
        return id;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void addItem(Item item, int quantity) {
        items.add(new OrderItem(id, item.getTitle(), item.getPrice(), quantity));
    }
}
