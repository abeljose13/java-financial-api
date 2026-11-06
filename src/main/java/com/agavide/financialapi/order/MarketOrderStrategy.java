package com.agavide.financialapi.order;

import com.agavide.financialapi.asset.Instrument;
import com.agavide.financialapi.asset.MarketData;
import com.agavide.financialapi.asset.MarketDataRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class MarketOrderStrategy implements OrderTypeStrategy {

	private final MarketDataRepository marketDataRepository;

	MarketOrderStrategy(MarketDataRepository marketDataRepository) {
		this.marketDataRepository = marketDataRepository;
	}

	@Override
	public OrderType type() {
		return OrderType.MARKET;
	}

	/**
	 * Uses the latest close. Returns zero when there is no market data, which the
	 * service treats as a business rejection (see decisions.md D-3).
	 */
	@Override
	public BigDecimal resolvePrice(SendOrderCommand command, Instrument instrument) {
		return marketDataRepository.findLatestByInstrumentId(instrument.getId())
				.map(MarketData::getClose)
				.orElse(BigDecimal.ZERO);
	}

	@Override
	public OrderStatus initialStatus() {
		return OrderStatus.FILLED;
	}
}
