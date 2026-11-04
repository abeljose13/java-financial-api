package com.agavide.financialapi.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Asset information returned by the search endpoint")
public record AssetResponse(
		@Schema(description = "Instrument internal id", example = "1")
		Integer id,

		@Schema(description = "Ticker symbol", example = "TEST")
		String ticker,

		@Schema(description = "Instrument name", example = "Test Company S.A.")
		String name,

		@Schema(description = "Instrument type", example = "ACCIONES")
		String type,

		@Schema(description = "Last available closing price in ARS", example = "1100.00")
		BigDecimal lastPrice,

		@Schema(description = "Daily return percentage based on close and previousClose", example = "4.76")
		BigDecimal dailyReturnPercentage
) {
}
