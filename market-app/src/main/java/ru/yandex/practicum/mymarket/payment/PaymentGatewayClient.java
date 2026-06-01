package ru.yandex.practicum.mymarket.payment;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.payment.client.model.BalanceResponse;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentRequest;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentResponse;

@Component
public class PaymentGatewayClient {

    public static final String USERNAME_HEADER = "X-User-Name";

    private final WebClient paymentWebClient;

    public PaymentGatewayClient(WebClient paymentWebClient) {
        this.paymentWebClient = paymentWebClient;
    }

    public Mono<BalanceResponse> getBalance(String username) {
        return paymentWebClient.get()
                .uri("/payments/balance")
                .header(USERNAME_HEADER, username)
                .retrieve()
                .bodyToMono(BalanceResponse.class);
    }

    public Mono<PaymentResponse> pay(String username, PaymentRequest request) {
        return paymentWebClient.post()
                .uri("/payments/pay")
                .header(USERNAME_HEADER, username)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponse.class);
    }
}
