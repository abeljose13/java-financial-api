package com.agavide.financialapi.portfolio;

import com.agavide.financialapi.order.Order;
import com.agavide.financialapi.order.OrderSide;
import com.agavide.financialapi.shared.domain.Money;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Holdings {

	private final Money cash;
	private final Map<Integer, Integer> sharesByInstrument;

	public Holdings(List<Order> filledOrders) {
		Money cashAccumulator = Money.zero();
		Map<Integer, Integer> sharesAccumulator = new HashMap<>();

		for (Order order : filledOrders) {
			if (!order.isFilled()) {
				continue;
			}

			if (order.isCashMovement()) {
				if (order.getSide() == OrderSide.CASH_IN) {
					cashAccumulator = cashAccumulator.add(order.totalAmount());
				} else {
					cashAccumulator = cashAccumulator.subtract(order.totalAmount());
				}
			} else if (order.isBuy()) {
				cashAccumulator = cashAccumulator.subtract(order.totalAmount());
				sharesAccumulator.merge(order.getInstrument().getId(), order.getSize(), Integer::sum);
			} else if (order.isSell()) {
				cashAccumulator = cashAccumulator.add(order.totalAmount());
				sharesAccumulator.merge(order.getInstrument().getId(), -order.getSize(), Integer::sum);
			}
		}

		this.cash = cashAccumulator;
		this.sharesByInstrument = Collections.unmodifiableMap(sharesAccumulator);
	}

	public Money cash() {
		return cash;
	}

	public int sharesOf(Integer instrumentId) {
		return sharesByInstrument.getOrDefault(instrumentId, 0);
	}

	public Map<Integer, Integer> sharesByInstrument() {
		return sharesByInstrument;
	}
}
