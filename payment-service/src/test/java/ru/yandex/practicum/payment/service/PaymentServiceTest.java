package ru.yandex.practicum.payment.service;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.payment.config.PaymentProperties;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTest {

	@Test
	void shouldReturnInitialBalance() {
		PaymentService paymentService = new PaymentService(new PaymentProperties(10000));

		assertThat(paymentService.getBalance("user").block()).isEqualTo(10000);
	}

	@Test
	void shouldDecreaseBalanceAfterSuccessfulPayment() {
		PaymentService paymentService = new PaymentService(new PaymentProperties(10000));

		PaymentService.PaymentResult result = paymentService.pay("user", 2500).block();

		assertThat(result).isNotNull();
		assertThat(result.success()).isTrue();
		assertThat(result.balance()).isEqualTo(7500);
		assertThat(paymentService.getBalance("user").block()).isEqualTo(7500);
	}

	@Test
	void shouldRejectPaymentWhenBalanceIsNotEnough() {
		PaymentService paymentService = new PaymentService(new PaymentProperties(1000));

		PaymentService.PaymentResult result = paymentService.pay("user", 2500).block();

		assertThat(result).isNotNull();
		assertThat(result.success()).isFalse();
		assertThat(result.balance()).isEqualTo(1000);
		assertThat(result.message()).isEqualTo("Недостаточно средств");
		assertThat(paymentService.getBalance("user").block()).isEqualTo(1000);
	}

	@Test
	void shouldKeepSeparateBalancesForDifferentUsers() {
		PaymentService paymentService = new PaymentService(new PaymentProperties(10000));

		PaymentService.PaymentResult firstUserPayment = paymentService.pay("user-a", 2500).block();
		PaymentService.PaymentResult secondUserPayment = paymentService.pay("user-b", 1000).block();

		assertThat(firstUserPayment).isNotNull();
		assertThat(firstUserPayment.balance()).isEqualTo(7500);
		assertThat(secondUserPayment).isNotNull();
		assertThat(secondUserPayment.balance()).isEqualTo(9000);
		assertThat(paymentService.getBalance("user-a").block()).isEqualTo(7500);
		assertThat(paymentService.getBalance("user-b").block()).isEqualTo(9000);
	}
}
