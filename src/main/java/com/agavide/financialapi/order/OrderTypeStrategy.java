package com.agavide.financialapi.order;

import com.agavide.financialapi.asset.Instrument;

import java.math.BigDecimal;

/**
 * Behavior that depends on the order type: how the execution price is resolved
 * and which status a valid order receives on creation.
 *
 * <p>Each implementation must be a Spring bean. {@link OrderService} collects all of them
 * and dispatches by {@link #type()}.
 */
public interface OrderTypeStrategy {

	OrderType type();

	BigDecimal resolvePrice(SendOrderCommand command, Instrument instrument);

	OrderStatus initialStatus();
}
