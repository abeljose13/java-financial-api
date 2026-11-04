package com.agavide.financialapi.order.dto;

import com.agavide.financialapi.order.OrderSide;
import com.agavide.financialapi.order.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Order submission request")
public record OrderRequest(
		@Schema(description = "Instrument internal id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
		Integer instrumentId,

		@Schema(description = "Order type: MARKET or LIMIT", example = "MARKET", requiredMode = Schema.RequiredMode.REQUIRED)
		OrderType type,

		@Schema(description = "Order side: BUY or SELL", example = "BUY", requiredMode = Schema.RequiredMode.REQUIRED)
		OrderSide side,

		@Schema(description = "Quantity of shares. Required for SELL; for BUY can be omitted if amountArs is provided")
		Integer quantity,

		@Schema(description = "Amount in ARS to invest (BUY only). Shares are computed as floor(amountArs / price)")
		BigDecimal amountArs,

		@Schema(description = "Limit price in ARS (required when type = LIMIT)")
		BigDecimal limitPrice
) {
}
