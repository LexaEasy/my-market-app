package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.AppUser;
import ru.yandex.practicum.mymarket.repository.AppUserRepository;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public Mono<AppUser> findOrCreateByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .switchIfEmpty(appUserRepository.save(AppUser.active(username)));
    }
}
