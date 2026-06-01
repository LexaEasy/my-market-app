package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.config.SecurityConfig;
import ru.yandex.practicum.payment.service.PaymentService;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerWebFluxTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private PaymentService paymentService;

	@Test
	void shouldReturnBalance() {
		when(paymentService.getBalance()).thenReturn(Mono.just(10000L));

		webTestClient.mutateWith(mockJwt())
				.get()
				.uri("/payments/balance")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.balance").isEqualTo(10000);

		verify(paymentService).getBalance();
	}

	@Test
	void shouldPaySuccessfully() {
		when(paymentService.pay(2500L))
				.thenReturn(Mono.just(PaymentService.PaymentResult.success(7500L)));

		webTestClient.mutateWith(mockJwt())
				.post()
				.uri("/payments/pay")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(Map.of("amount", 2500))
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.success").isEqualTo(true)
				.jsonPath("$.balance").isEqualTo(7500)
				.jsonPath("$.message").doesNotExist();

		verify(paymentService).pay(2500L);
	}

	@Test
	void shouldRejectPaymentWhenBalanceIsNotEnough() {
		when(paymentService.pay(2500L))
				.thenReturn(Mono.just(PaymentService.PaymentResult.failed(1000L, "Недостаточно средств")));

		webTestClient.mutateWith(mockJwt())
				.post()
				.uri("/payments/pay")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(Map.of("amount", 2500))
				.exchange()
				.expectStatus().isEqualTo(409)
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.success").isEqualTo(false)
				.jsonPath("$.balance").isEqualTo(1000)
				.jsonPath("$.message").isEqualTo("Недостаточно средств");

		verify(paymentService).pay(2500L);
	}

	@Test
	void shouldRejectBalanceWithoutToken() {
		webTestClient.get()
				.uri("/payments/balance")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void shouldRejectPaymentWithoutToken() {
		webTestClient.post()
				.uri("/payments/pay")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(Map.of("amount", 2500))
				.exchange()
				.expectStatus().isUnauthorized();
	}
}
