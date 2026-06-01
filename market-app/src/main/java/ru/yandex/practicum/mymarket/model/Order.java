package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;

@Table("orders")
public class Order {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Transient
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public static Order create(Long userId) {
        Order order = new Order();
        order.userId = userId;
        return order;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void addItem(Item item, int quantity) {
        items.add(new OrderItem(id, item.getTitle(), item.getPrice(), quantity));
    }
}
