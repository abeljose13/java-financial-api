package com.agavide.financialapi.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTransitionTest {

	@Test
	void newLimitOrderShouldBeInStatusNew() {
		Order order = Order.newLimitBuy(null, null, 10, BigDecimal.valueOf(100), LocalDateTime.now());
		assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
		assertThat(order.isNew()).isTrue();
	}

	@Test
	void marketBuyShouldBeFilled() {
		Order order = Order.newMarketBuy(null, null, 10, BigDecimal.valueOf(100), LocalDateTime.now());
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
		Order order = Order.newLimitBuy(null, null, 10, BigDecimal.valueOf(100), LocalDateTime.now());
		order.cancel();
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void filledOrderCannotBeCancelled() {
		Order order = Order.newMarketBuy(null, null, 10, BigDecimal.valueOf(100), LocalDateTime.now());
		assertThatThrownBy(order::cancel)
			.isInstanceOf(com.agavide.financialapi.shared.domain.DomainException.class)
			.hasMessageContaining("Only NEW orders can be cancelled");
	}
}
