package com.agavide.financialapi.order;

import com.agavide.financialapi.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTransitionTest {

	@Test
	void newLimitOrderShouldBeInStatusNew() {
		Order order = limit(OrderSide.BUY, OrderStatus.NEW);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
		assertThat(order.isNew()).isTrue();
	}

	@Test
	void marketBuyShouldBeFilled() {
		Order order = market(OrderSide.BUY, OrderStatus.FILLED);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
		assertThat(order.isFilled()).isTrue();
		assertThat(order.affectsPosition()).isTrue();
	}

	@Test
	void cashMovementShouldNotAffectPosition() {
		Order order = Order.newCashIn(null, null, 1000, LocalDateTime.now());
		assertThat(order.affectsCash()).isTrue();
		assertThat(order.affectsPosition()).isFalse();
	}

	@Test
	void cancelShouldMoveToCancelled() {
		Order order = limit(OrderSide.BUY, OrderStatus.NEW);
		order.cancel();
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void filledOrderCannotBeCancelled() {
		Order order = market(OrderSide.BUY, OrderStatus.FILLED);
		assertThatThrownBy(order::cancel)
			.isInstanceOf(ConflictException.class)
			.hasMessage("Order cannot be cancelled because it is FILLED");
	}

	private static Order market(OrderSide side, OrderStatus status) {
		return Order.create(null, null, 10, BigDecimal.valueOf(100),
				OrderType.MARKET, side, status, LocalDateTime.now());
	}

	private static Order limit(OrderSide side, OrderStatus status) {
		return Order.create(null, null, 10, BigDecimal.valueOf(100),
				OrderType.LIMIT, side, status, LocalDateTime.now());
	}
}
