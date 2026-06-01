package ru.yandex.practicum.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.generated.api.PaymentsApi;
import ru.yandex.practicum.payment.generated.model.BalanceResponse;
import ru.yandex.practicum.payment.generated.model.PaymentRequest;
import ru.yandex.practicum.payment.generated.model.PaymentResponse;
import ru.yandex.practicum.payment.service.PaymentService;

@RestController
public class PaymentController implements PaymentsApi {

	public static final String USERNAME_HEADER = "X-User-Name";

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@Override
	public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
		String username = usernameFrom(exchange);
		if (username == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}
		return paymentService.getBalance(username)
				.map(balance -> ResponseEntity.ok(new BalanceResponse(balance)));
	}

	@Override
	public Mono<ResponseEntity<PaymentResponse>> pay(
			Mono<PaymentRequest> paymentRequest,
			ServerWebExchange exchange
	) {
		String username = usernameFrom(exchange);
		if (username == null) {
			return Mono.just(ResponseEntity.badRequest().build());
		}
		return paymentRequest
				.flatMap(request -> paymentService.pay(username, request.getAmount()))
				.map(result -> {
					PaymentResponse response = new PaymentResponse(result.success(), result.balance())
							.message(result.message());
					HttpStatus status = result.success() ? HttpStatus.OK : HttpStatus.CONFLICT;
					return ResponseEntity.status(status).body(response);
				});
	}

	private String usernameFrom(ServerWebExchange exchange) {
		String username = exchange.getRequest().getHeaders().getFirst(USERNAME_HEADER);
		return username == null || username.isBlank() ? null : username;
	}
}
