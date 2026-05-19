package ru.yandex.practicum.mymarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;
import ru.yandex.practicum.mymarket.payment.client.model.BalanceResponse;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentRequest;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentClientServiceTest {

    @Mock
    private PaymentsApi paymentsApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnAvailableBalance() {
        PaymentClientService service = new PaymentClientService(paymentsApi, objectMapper);
        when(paymentsApi.getBalance()).thenReturn(Mono.just(new BalanceResponse().balance(10000L)));

        StepVerifier.create(service.getBalance())
                .expectNextMatches(balance -> balance.available() && balance.balance() == 10000L)
                .verifyComplete();
    }

    @Test
    void shouldReturnUnavailableBalanceWhenPaymentServiceFails() {
        PaymentClientService service = new PaymentClientService(paymentsApi, objectMapper);
        when(paymentsApi.getBalance()).thenReturn(Mono.error(new IllegalStateException("Connection refused")));

        StepVerifier.create(service.getBalance())
                .expectNextMatches(balance -> !balance.available()
                        && balance.message().equals("Сервис платежей недоступен"))
                .verifyComplete();
    }

    @Test
    void shouldPaySuccessfully() {
        PaymentClientService service = new PaymentClientService(paymentsApi, objectMapper);
        when(paymentsApi.pay(org.mockito.ArgumentMatchers.any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse().success(true).balance(7500L)));

        StepVerifier.create(service.pay(2500L))
                .expectNextMatches(result -> result.success()
                        && result.serviceAvailable()
                        && result.balance() == 7500L)
                .verifyComplete();

        ArgumentCaptor<PaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentsApi).pay(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAmount()).isEqualTo(2500L);
    }

    @Test
    void shouldReturnRejectedPaymentOnConflict() throws Exception {
        PaymentClientService service = new PaymentClientService(paymentsApi, objectMapper);
        byte[] body = objectMapper.writeValueAsBytes(new PaymentResponse()
                .success(false)
                .balance(1000L)
                .message("Недостаточно средств"));
        when(paymentsApi.pay(org.mockito.ArgumentMatchers.any(PaymentRequest.class)))
                .thenReturn(Mono.error(WebClientResponseException.create(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        HttpHeaders.EMPTY,
                        body,
                        StandardCharsets.UTF_8
                )));

        StepVerifier.create(service.pay(2500L))
                .expectNextMatches(result -> !result.success()
                        && result.serviceAvailable()
                        && result.balance() == 1000L
                        && result.message().equals("Недостаточно средств"))
                .verifyComplete();
    }
}
