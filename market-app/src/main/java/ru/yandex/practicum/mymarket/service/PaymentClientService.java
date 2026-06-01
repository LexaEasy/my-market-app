package ru.yandex.practicum.mymarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.OrderPaymentResult;
import ru.yandex.practicum.mymarket.dto.PaymentAvailability;
import ru.yandex.practicum.mymarket.payment.PaymentGatewayClient;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentRequest;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentResponse;

@Service
public class PaymentClientService {

    private static final String PAYMENT_SERVICE_UNAVAILABLE = "Сервис платежей недоступен";
    private static final String PAYMENT_REJECTED = "Платёж не выполнен";

    private final PaymentGatewayClient paymentGatewayClient;
    private final ObjectMapper objectMapper;

    public PaymentClientService(PaymentGatewayClient paymentGatewayClient, ObjectMapper objectMapper) {
        this.paymentGatewayClient = paymentGatewayClient;
        this.objectMapper = objectMapper;
    }

    public Mono<PaymentAvailability> getBalance(String username) {
        return paymentGatewayClient.getBalance(username)
                .map(response -> PaymentAvailability.available(response.getBalance()))
                .onErrorResume(ClientAuthorizationException.class, this::handleBalanceAuthorizationError)
                .onErrorResume(WebClientResponseException.class, this::handleBalanceResponseError)
                .onErrorResume(WebClientRequestException.class, this::handleBalanceRequestError);
    }

    public Mono<OrderPaymentResult> pay(String username, long amount) {
        PaymentRequest request = new PaymentRequest().amount(amount);
        return paymentGatewayClient.pay(username, request)
                .map(this::toPaymentResult)
                .onErrorResume(ClientAuthorizationException.class, this::handlePaymentAuthorizationError)
                .onErrorResume(WebClientResponseException.class, this::handlePaymentError)
                .onErrorResume(WebClientRequestException.class, this::handlePaymentRequestError);
    }

    private Mono<PaymentAvailability> handleBalanceAuthorizationError(ClientAuthorizationException error) {
        return Mono.just(PaymentAvailability.unavailable(PAYMENT_SERVICE_UNAVAILABLE));
    }

    private Mono<PaymentAvailability> handleBalanceRequestError(WebClientRequestException error) {
        return Mono.just(PaymentAvailability.unavailable(PAYMENT_SERVICE_UNAVAILABLE));
    }

    private Mono<PaymentAvailability> handleBalanceResponseError(WebClientResponseException error) {
        return Mono.just(PaymentAvailability.unavailable(PAYMENT_SERVICE_UNAVAILABLE));
    }

    private Mono<OrderPaymentResult> handlePaymentRequestError(WebClientRequestException error) {
        return Mono.just(OrderPaymentResult.unavailable(PAYMENT_SERVICE_UNAVAILABLE));
    }

    private Mono<OrderPaymentResult> handlePaymentAuthorizationError(ClientAuthorizationException error) {
        return Mono.just(OrderPaymentResult.unavailable(PAYMENT_SERVICE_UNAVAILABLE));
    }

    private OrderPaymentResult toPaymentResult(PaymentResponse response) {
        if (Boolean.TRUE.equals(response.getSuccess())) {
            return OrderPaymentResult.success(response.getBalance());
        }
        return OrderPaymentResult.rejected(response.getBalance(), resolveMessage(response.getMessage()));
    }

    private Mono<OrderPaymentResult> handlePaymentError(WebClientResponseException error) {
        if (error.getStatusCode() == HttpStatus.CONFLICT) {
            return Mono.just(toPaymentResult(parsePaymentResponse(error)));
        }
        return Mono.just(OrderPaymentResult.unavailable(PAYMENT_SERVICE_UNAVAILABLE));
    }

    private PaymentResponse parsePaymentResponse(WebClientResponseException error) {
        try {
            return objectMapper.readValue(error.getResponseBodyAsByteArray(), PaymentResponse.class);
        } catch (Exception ignored) {
            return new PaymentResponse()
                    .success(false)
                    .balance(0L)
                    .message(PAYMENT_REJECTED);
        }
    }

    private String resolveMessage(String message) {
        return message == null || message.isBlank() ? PAYMENT_REJECTED : message;
    }
}
