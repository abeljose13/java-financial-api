package com.agavide.financialapi.portfolio;

import com.agavide.financialapi.portfolio.dto.PortfolioResponse;
import com.agavide.financialapi.portfolio.dto.PositionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/portfolio")
@Tag(name = "Portfolio", description = "User portfolio valuation and positions")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@GetMapping
	@Operation(
			summary = "Get user portfolio",
			description = "Returns total account value, cash balance and open positions calculated from FILLED orders and latest market data."
	)
	@ApiResponse(responseCode = "200", description = "Portfolio summary",
			content = @Content(mediaType = "application/json",
					schema = @Schema(implementation = PortfolioResponse.class)))
	@ApiResponse(responseCode = "404", description = "User not found")
	public ResponseEntity<PortfolioResponse> getPortfolio(
			@Parameter(description = "User internal id", example = "1")
			@PathVariable Integer userId) {
		Portfolio portfolio = portfolioService.getPortfolio(userId);
		return ResponseEntity.ok(toResponse(portfolio));
	}

	private PortfolioResponse toResponse(Portfolio portfolio) {
		List<PositionResponse> positions = portfolio.getPositions().stream()
				.map(this::toResponse)
				.toList();

		return new PortfolioResponse(
				portfolio.getUserId(),
				portfolio.getTotalAccountValue().raw(),
				portfolio.getCashBalance().raw(),
				portfolio.getAvailableCash().raw(),
				positions
		);
	}

	private PositionResponse toResponse(Position position) {
		return new PositionResponse(
				position.getInstrumentId(),
				position.getTicker(),
				position.getName(),
				position.getQuantity(),
				position.getAverageCost() != null ? position.getAverageCost().raw() : null,
				position.getMarketPrice(),
				position.getMarketValue() != null ? position.getMarketValue().raw() : null,
				position.getDailyReturnPercentage(),
				position.getTotalReturnPercentage()
		);
	}
}
