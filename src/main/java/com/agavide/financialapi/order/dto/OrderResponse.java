package com.agavide.financialapi.order.dto;

import com.agavide.financialapi.order.OrderSide;
import com.agavide.financialapi.order.OrderStatus;
import com.agavide.financialapi.order.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Order submission response")
public record OrderResponse(
		@Schema(description = "Order internal id", example = "42")
		Integer orderId,

		@Schema(description = "Current order status", example = "FILLED")
		OrderStatus status,

		@Schema(description = "Instrument internal id", example = "1")
		Integer instrumentId,

		@Schema(description = "Ticker symbol", example = "TEST")
		String ticker,

		@Schema(description = "Order side", example = "BUY")
		OrderSide side,

		@Schema(description = "Order type", example = "MARKET")
		OrderType type,

		@Schema(description = "Size in shares (or ARS amount for cash movements)", example = "10")
		Integer size,

		@Schema(description = "Order price in ARS per share", example = "1100.00")
		BigDecimal price,

		@Schema(description = "Execution price in ARS per share. For MARKET equals market close; for LIMIT equals limitPrice", example = "1100.00")
		BigDecimal executionPrice,

		@Schema(description = "Order timestamp", example = "2026-11-04T12:34:56")
		LocalDateTime datetime
) {
}
