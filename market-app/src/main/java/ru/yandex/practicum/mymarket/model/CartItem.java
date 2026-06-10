package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("cart_items")
public class CartItem {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("item_id")
    private Long itemId;

    @Transient
    private Item item;

    private int quantity;

    protected CartItem() {
    }

    public CartItem(Long userId, Item item, int quantity) {
        this.userId = userId;
        this.item = item;
        this.itemId = item.getId();
        this.quantity = quantity;
    }

    public CartItem(Long userId, Long itemId, int quantity) {
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getItemId() {
        return itemId;
    }

    public Item getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void increase() {
        quantity++;
    }

    public void decrease() {
        if (quantity > 0) {
            quantity--;
        }
    }
}
