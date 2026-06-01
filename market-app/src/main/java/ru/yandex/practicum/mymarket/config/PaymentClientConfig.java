package ru.yandex.practicum.mymarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.payment.client.ApiClient;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;

@Configuration
public class PaymentClientConfig {

    private static final String PAYMENT_SERVICE_REGISTRATION_ID = "payment-service";
    private static final Authentication PAYMENT_CLIENT_PRINCIPAL = new AnonymousAuthenticationToken(
            "payment-service-client",
            "payment-service-client",
            AuthorityUtils.createAuthorityList("ROLE_SYSTEM")
    );

    @Bean
    public ReactiveOAuth2AuthorizedClientManager paymentAuthorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService
    ) {
        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService
                );
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    @Bean
    public WebClient paymentWebClient(ReactiveOAuth2AuthorizedClientManager paymentAuthorizedClientManager) {
        return ApiClient.buildWebClientBuilder(ApiClient.createDefaultMapper(null))
                .filter(paymentAuthorizationFilter(paymentAuthorizedClientManager))
                .build();
    }

    @Bean
    public ApiClient paymentApiClient(PaymentClientProperties properties, WebClient paymentWebClient) {
        return new ApiClient(paymentWebClient).setBasePath(properties.baseUrl());
    }

    @Bean
    public PaymentsApi paymentsApi(ApiClient paymentApiClient) {
        return new PaymentsApi(paymentApiClient);
    }

    private ExchangeFilterFunction paymentAuthorizationFilter(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager
    ) {
        return (request, next) -> authorizedClientManager.authorize(OAuth2AuthorizeRequest
                        .withClientRegistrationId(PAYMENT_SERVICE_REGISTRATION_ID)
                        .principal(PAYMENT_CLIENT_PRINCIPAL)
                        .build())
                .switchIfEmpty(Mono.error(() -> authorizationError()))
                .flatMap(authorizedClient -> next.exchange(ClientRequest.from(request)
                        .headers(headers -> headers.setBearerAuth(
                                authorizedClient.getAccessToken().getTokenValue()
                        ))
                        .build()));
    }

    private ClientAuthorizationException authorizationError() {
        return new ClientAuthorizationException(
                new OAuth2Error(
                        "payment_authorization_failed",
                        "Payment service authorization failed",
                        null
                ),
                PAYMENT_SERVICE_REGISTRATION_ID
        );
    }
}
