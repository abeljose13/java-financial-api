package com.agavide.financialapi.order;

import java.math.BigDecimal;

public record SendOrderCommand(
		Integer userId,
		Integer instrumentId,
		OrderType type,
		OrderSide side,
		Integer quantity,
		BigDecimal amountArs,
		BigDecimal limitPrice
) {
}
