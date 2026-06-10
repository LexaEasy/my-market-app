package ru.yandex.practicum.mymarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final ReactiveClientRegistrationRepository clientRegistrationRepository;
    private final String keycloakLogoutUri;

    public SecurityConfig(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            @Value("${app.security.keycloak-logout-uri}") String keycloakLogoutUri
    ) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.keycloakLogoutUri = keycloakLogoutUri;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/items", "/items/*").authenticated()
                        .pathMatchers(HttpMethod.GET, "/", "/items", "/items/*").permitAll()
                        .pathMatchers("/login", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .pathMatchers("/cart/**", "/orders/**", "/buy", "/logout").authenticated()
                        .anyExchange().permitAll()
                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                )
                .build();
    }

    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedServerLogoutSuccessHandler successHandler =
                new OidcClientInitiatedServerLogoutSuccessHandler(oidcLogoutClientRegistrationRepository());
        successHandler.setPostLogoutRedirectUri("{baseUrl}/");
        return successHandler;
    }

    private ReactiveClientRegistrationRepository oidcLogoutClientRegistrationRepository() {
        return registrationId -> clientRegistrationRepository.findByRegistrationId(registrationId)
                .map(this::withEndSessionEndpoint);
    }

    private ClientRegistration withEndSessionEndpoint(ClientRegistration clientRegistration) {
        Map<String, Object> metadata = new HashMap<>(
                clientRegistration.getProviderDetails().getConfigurationMetadata()
        );
        metadata.put("end_session_endpoint", keycloakLogoutUri);

        return ClientRegistration.withClientRegistration(clientRegistration)
                .providerConfigurationMetadata(metadata)
                .build();
    }
}
