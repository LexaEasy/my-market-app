package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("cart_items")
public class CartItem {

    @Id
    private Long id;

    @Column("item_id")
    private Long itemId;

    @Transient
    private Item item;

    private int quantity;

    protected CartItem() {
    }

    public CartItem(Item item, int quantity) {
        this.item = item;
        this.itemId = item.getId();
        this.quantity = quantity;
    }

    public CartItem(Long itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
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
