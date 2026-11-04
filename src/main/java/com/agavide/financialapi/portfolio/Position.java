package com.agavide.financialapi.portfolio;

import com.agavide.financialapi.order.Order;
import com.agavide.financialapi.shared.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Position {

	private static final int RETURN_SCALE = 2;
	private static final RoundingMode RETURN_ROUNDING = RoundingMode.HALF_UP;

	private final Integer instrumentId;
	private final String ticker;
	private final String name;
	private final int quantity;
	private final Money averageCost;
	private final BigDecimal marketPrice;
	private final Money marketValue;
	private final BigDecimal dailyReturnPercentage;
	private final BigDecimal totalReturnPercentage;

	public Position(Integer instrumentId, String ticker, String name, int quantity,
					Money averageCost, BigDecimal marketPrice, Money marketValue,
					BigDecimal dailyReturnPercentage, BigDecimal totalReturnPercentage) {
		this.instrumentId = instrumentId;
		this.ticker = ticker;
		this.name = name;
		this.quantity = quantity;
		this.averageCost = averageCost;
		this.marketPrice = marketPrice;
		this.marketValue = marketValue;
		this.dailyReturnPercentage = dailyReturnPercentage;
		this.totalReturnPercentage = totalReturnPercentage;
	}

	public static Position fromOrders(Integer instrumentId, String ticker, String name,
									  List<Order> filledOrders, BigDecimal close, BigDecimal previousClose) {
		int shares = 0;
		Money costBasis = Money.zero();

		for (Order order : filledOrders) {
			Money price = Money.of(order.getPrice());
			if (order.isBuy()) {
				Money purchaseCost = price.multiply(order.getSize());
				costBasis = costBasis.add(purchaseCost);
				shares += order.getSize();
			} else if (order.isSell()) {
				Money soldCost = costOfSoldShares(costBasis, shares, order.getSize());
				costBasis = costBasis.subtract(soldCost);
				shares -= order.getSize();
			}
		}

		if (shares <= 0) {
			return null;
		}

		Money avgCost = averageCostAtMoment(costBasis, shares);
		Money marketValue = close != null ? Money.of(close).multiply(shares) : null;
		BigDecimal dailyReturn = calculateDailyReturn(close, previousClose);
		BigDecimal totalReturn = calculateTotalReturn(marketValue, costBasis);

		return new Position(instrumentId, ticker, name, shares, avgCost, close,
				marketValue, dailyReturn, totalReturn);
	}

	private static Money averageCostAtMoment(Money costBasis, int shares) {
		if (shares == 0) {
			return Money.zero();
		}
		return Money.of(costBasis.raw().divide(BigDecimal.valueOf(shares), 10, RoundingMode.HALF_UP));
	}

	private static Money costOfSoldShares(Money costBasis, int totalShares, int soldShares) {
		if (totalShares == 0) {
			return Money.zero();
		}
		BigDecimal exactCost = costBasis.raw()
				.multiply(BigDecimal.valueOf(soldShares))
				.divide(BigDecimal.valueOf(totalShares), 10, RoundingMode.HALF_UP);
		return Money.of(exactCost);
	}

	private static BigDecimal calculateDailyReturn(BigDecimal close, BigDecimal previousClose) {
		if (close == null || previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		return close.subtract(previousClose)
				.divide(previousClose, RETURN_SCALE + 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(RETURN_SCALE, RETURN_ROUNDING);
	}

	private static BigDecimal calculateTotalReturn(Money marketValue, Money costBasis) {
		if (marketValue == null || costBasis == null || costBasis.raw().compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		return marketValue.raw().subtract(costBasis.raw())
				.divide(costBasis.raw(), RETURN_SCALE + 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(RETURN_SCALE, RETURN_ROUNDING);
	}

	public Integer getInstrumentId() {
		return instrumentId;
	}

	public String getTicker() {
		return ticker;
	}

	public String getName() {
		return name;
	}

	public int getQuantity() {
		return quantity;
	}

	public Money getAverageCost() {
		return averageCost;
	}

	public BigDecimal getMarketPrice() {
		return marketPrice;
	}

	public Money getMarketValue() {
		return marketValue;
	}

	public BigDecimal getDailyReturnPercentage() {
		return dailyReturnPercentage;
	}

	public BigDecimal getTotalReturnPercentage() {
		return totalReturnPercentage;
	}
}
