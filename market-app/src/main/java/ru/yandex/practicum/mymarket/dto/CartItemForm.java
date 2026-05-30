package ru.yandex.practicum.mymarket.dto;

import ru.yandex.practicum.mymarket.model.CartAction;

public class CartItemForm {

    private Long id;
    private String search;
    private String sort;
    private Integer pageNumber;
    private Integer pageSize;
    private CartAction action;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public CartAction getAction() {
        return action;
    }

    public void setAction(CartAction action) {
        this.action = action;
    }
}
