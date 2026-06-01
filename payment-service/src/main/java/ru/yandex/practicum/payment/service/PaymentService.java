package ru.yandex.practicum.payment.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.config.PaymentProperties;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class PaymentService {

	private static final String NOT_ENOUGH_MONEY_MESSAGE = "Недостаточно средств";

	private final AtomicLong balance;

	public PaymentService(PaymentProperties properties) {
		this.balance = new AtomicLong(properties.initialBalance());
	}

	public Mono<Long> getBalance() {
		return Mono.fromSupplier(balance::get);
	}

	public Mono<Long> getBalance(String username) {
		return getBalance();
	}

	public Mono<PaymentResult> pay(long amount) {
		return Mono.fromSupplier(() -> {
			while (true) {
				long currentBalance = balance.get();
				if (currentBalance < amount) {
					return PaymentResult.failed(currentBalance, NOT_ENOUGH_MONEY_MESSAGE);
				}

				long updatedBalance = currentBalance - amount;
				if (balance.compareAndSet(currentBalance, updatedBalance)) {
					return PaymentResult.success(updatedBalance);
				}
			}
		});
	}

	public Mono<PaymentResult> pay(String username, long amount) {
		return pay(amount);
	}

	public record PaymentResult(boolean success, long balance, String message) {

		public static PaymentResult success(long balance) {
			return new PaymentResult(true, balance, null);
		}

		public static PaymentResult failed(long balance, String message) {
			return new PaymentResult(false, balance, message);
		}
	}
}
