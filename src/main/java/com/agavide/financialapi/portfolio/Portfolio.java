package com.agavide.financialapi.portfolio;

import com.agavide.financialapi.shared.domain.Money;

import java.util.List;

public class Portfolio {

	private final Integer userId;
	private final Money cashBalance;
	private final Money availableCash;
	private final Money totalAccountValue;
	private final List<Position> positions;

	public Portfolio(Integer userId, Money cashBalance, List<Position> positions) {
		this.userId = userId;
		this.cashBalance = cashBalance;
		this.availableCash = cashBalance;
		this.positions = List.copyOf(positions);
		this.totalAccountValue = calculateTotalValue(cashBalance, positions);
	}

	private static Money calculateTotalValue(Money cash, List<Position> positions) {
		Money total = cash;
		for (Position position : positions) {
			if (position.getMarketValue() != null) {
				total = total.add(position.getMarketValue());
			}
		}
		return total;
	}

	public Integer getUserId() {
		return userId;
	}

	public Money getCashBalance() {
		return cashBalance;
	}

	public Money getAvailableCash() {
		return availableCash;
	}

	public Money getTotalAccountValue() {
		return totalAccountValue;
	}

	public List<Position> getPositions() {
		return positions;
	}
}
