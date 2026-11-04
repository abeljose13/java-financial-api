package com.agavide.financialapi.portfolio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Open position of a user in a single instrument")
public record PositionResponse(
		@Schema(description = "Instrument internal id", example = "1")
		Integer instrumentId,

		@Schema(description = "Ticker symbol", example = "TEST")
		String ticker,

		@Schema(description = "Instrument name", example = "Test Company S.A.")
		String name,

		@Schema(description = "Number of shares held", example = "10")
		Integer quantity,

		@Schema(description = "Average acquisition cost per share in ARS", example = "1000.00")
		BigDecimal averageCost,

		@Schema(description = "Last market price in ARS", example = "1100.00")
		BigDecimal marketPrice,

		@Schema(description = "Total market value of the position in ARS", example = "11000.00")
		BigDecimal marketValue,

		@Schema(description = "Daily return percentage", example = "4.76")
		BigDecimal dailyReturnPercentage,

		@Schema(description = "Unrealized return percentage of the open position", example = "10.00")
		BigDecimal totalReturnPercentage
) {
}
