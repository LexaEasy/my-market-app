package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public class AppUser {

    @Id
    private Long id;

    private String username;

    private boolean enabled;

    protected AppUser() {
    }

    public AppUser(Long id, String username, boolean enabled) {
        this.id = id;
        this.username = username;
        this.enabled = enabled;
    }

    public static AppUser active(String username) {
        return new AppUser(null, username, true);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
