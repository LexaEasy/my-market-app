package ru.yandex.practicum.payment.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.config.PaymentProperties;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PaymentService {

	private static final String NOT_ENOUGH_MONEY_MESSAGE = "Недостаточно средств";

	private final long initialBalance;
	private final ConcurrentMap<String, AtomicLong> balances = new ConcurrentHashMap<>();

	public PaymentService(PaymentProperties properties) {
		this.initialBalance = properties.initialBalance();
	}

	public Mono<Long> getBalance() {
		return getBalance("default");
	}

	public Mono<Long> getBalance(String username) {
		return Mono.fromSupplier(() -> balanceFor(username).get());
	}

	public Mono<PaymentResult> pay(long amount) {
		return pay("default", amount);
	}

	public Mono<PaymentResult> pay(String username, long amount) {
		return Mono.fromSupplier(() -> {
			AtomicLong balance = balanceFor(username);
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

	private AtomicLong balanceFor(String username) {
		return balances.computeIfAbsent(username, ignored -> new AtomicLong(initialBalance));
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
