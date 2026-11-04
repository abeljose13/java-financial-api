package com.agavide.financialapi.portfolio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Portfolio summary for a user")
public record PortfolioResponse(
		@Schema(description = "User internal id", example = "1")
		Integer userId,

		@Schema(description = "Total account value in ARS (cash + positions)", example = "98500.00")
		BigDecimal totalAccountValue,

		@Schema(description = "Cash balance in ARS", example = "80000.00")
		BigDecimal cashBalance,

		@Schema(description = "Cash available for trading in ARS. Equals cashBalance when no reservations are applied", example = "80000.00")
		BigDecimal availableCash,

		@Schema(description = "Open positions")
		List<PositionResponse> positions
) {
}
