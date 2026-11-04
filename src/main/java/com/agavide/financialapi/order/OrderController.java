package com.agavide.financialapi.order;

import com.agavide.financialapi.order.dto.OrderRequest;
import com.agavide.financialapi.order.dto.OrderResponse;
import com.agavide.financialapi.shared.domain.DomainException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/orders")
@Tag(name = "Orders", description = "Order submission (MARKET and LIMIT)")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@Operation(
			summary = "Submit a new order",
			description = """
					Creates a BUY or SELL order.
					MARKET orders are executed immediately at the latest close price and become FILLED.
					LIMIT orders are stored as NEW and validated against available resources.
					If the user has insufficient funds or shares, the order is persisted as REJECTED
					and the response returns HTTP 422.
					"""
	)
	@ApiResponse(responseCode = "200", description = "Order created (FILLED or NEW)",
			content = @Content(mediaType = "application/json",
					schema = @Schema(implementation = OrderResponse.class)))
	@ApiResponse(responseCode = "400", description = "Malformed request (e.g. missing quantity, invalid instrument type)")
	@ApiResponse(responseCode = "404", description = "User or instrument not found")
	@ApiResponse(responseCode = "422", description = "Business rejection persisted as REJECTED")
	public ResponseEntity<OrderResponse> submitOrder(
			@Parameter(description = "User internal id", example = "1")
			@PathVariable Integer userId,
			@RequestBody OrderRequest request) {
		validateRequest(request);

		SendOrderCommand command = new SendOrderCommand(
				userId,
				request.instrumentId(),
				request.type(),
				request.side(),
				request.quantity(),
				request.amountArs(),
				request.limitPrice()
		);

		Order order = orderService.sendOrder(command);
		HttpStatus status = order.isRejected() ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK;
		return ResponseEntity.status(status).body(toResponse(order));
	}

	@DeleteMapping("/{orderId}")
	@Operation(
			summary = "Cancel a NEW order",
			description = """
					Cancels an order that is still in NEW status.
					The order is not deleted; its status is updated to CANCELLED.
					Only the owner of the order can cancel it.
					"""
	)
	@ApiResponse(responseCode = "200", description = "Order cancelled",
			content = @Content(mediaType = "application/json",
					schema = @Schema(implementation = OrderResponse.class)))
	@ApiResponse(responseCode = "404", description = "Order not found for the user")
	@ApiResponse(responseCode = "409", description = "Order is not in NEW status")
	public ResponseEntity<OrderResponse> cancelOrder(
			@Parameter(description = "User internal id", example = "1")
			@PathVariable Integer userId,
			@Parameter(description = "Order internal id", example = "4")
			@PathVariable Integer orderId) {
		Order order = orderService.cancelOrder(userId, orderId);
		return ResponseEntity.ok(toResponse(order));
	}

	private void validateRequest(OrderRequest request) {
		if (request.instrumentId() == null) {
			throw new DomainException("instrumentId is required");
		}
		if (request.type() == null) {
			throw new DomainException("type is required");
		}
		if (request.side() == null) {
			throw new DomainException("side is required");
		}
	}

	private OrderResponse toResponse(Order order) {
		return new OrderResponse(
				order.getId(),
				order.getStatus(),
				order.getInstrument().getId(),
				order.getInstrument().getTicker(),
				order.getSide(),
				order.getType(),
				order.getSize(),
				order.getPrice(),
				order.getPrice(),
				order.getDatetime()
		);
	}
}
