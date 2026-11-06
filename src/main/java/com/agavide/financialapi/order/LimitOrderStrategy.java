package com.agavide.financialapi.order;

import com.agavide.financialapi.asset.Instrument;
import com.agavide.financialapi.shared.domain.DomainException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
class LimitOrderStrategy implements OrderTypeStrategy {

	private static final int PRICE_SCALE = 2;

	@Override
	public OrderType type() {
		return OrderType.LIMIT;
	}

	/**
	 * Uses the requested limit price, normalized to the same scale the database stores.
	 */
	@Override
	public BigDecimal resolvePrice(SendOrderCommand command, Instrument instrument) {
		BigDecimal limitPrice = command.limitPrice();
		if (limitPrice == null || limitPrice.signum() <= 0) {
			throw new DomainException("Limit price must be positive");
		}
		return limitPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
	}

	@Override
	public OrderStatus initialStatus() {
		return OrderStatus.NEW;
	}
}
