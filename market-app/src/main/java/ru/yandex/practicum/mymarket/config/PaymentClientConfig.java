package ru.yandex.practicum.mymarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.mymarket.payment.client.ApiClient;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;

@Configuration
public class PaymentClientConfig {

    @Bean
    public ApiClient paymentApiClient(PaymentClientProperties properties) {
        return new ApiClient().setBasePath(properties.baseUrl());
    }

    @Bean
    public PaymentsApi paymentsApi(ApiClient paymentApiClient) {
        return new PaymentsApi(paymentApiClient);
    }
}
