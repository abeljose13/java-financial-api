package com.agavide.financialapi.portfolio;

import com.agavide.financialapi.order.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PositionTest {

	@Test
	void shouldCalculatePositionFromBuyOrders() {
		Order buy = Order.newMarketBuy(null, null, 10, BigDecimal.valueOf(1000), LocalDateTime.now());

		Position position = Position.fromOrders(1, "TEST", "Test", List.of(buy),
				BigDecimal.valueOf(1100), BigDecimal.valueOf(1050));

		assertThat(position).isNotNull();
		assertThat(position.getQuantity()).isEqualTo(10);
		assertThat(position.getAverageCost().raw()).isEqualByComparingTo("1000.00");
		assertThat(position.getMarketValue().raw()).isEqualByComparingTo("11000.00");
		assertThat(position.getTotalReturnPercentage()).isEqualByComparingTo("10.00");
		assertThat(position.getDailyReturnPercentage()).isEqualByComparingTo("4.76");
	}

	@Test
	void shouldApplyWacWhenSellingPartially() {
		Order buy1 = Order.newMarketBuy(null, null, 10, BigDecimal.valueOf(1000), LocalDateTime.now());
		Order buy2 = Order.newMarketBuy(null, null, 10, BigDecimal.valueOf(1200), LocalDateTime.now());
		Order sell = Order.newMarketSell(null, null, 5, BigDecimal.valueOf(1300), LocalDateTime.now());

		Position position = Position.fromOrders(1, "TEST", "Test", List.of(buy1, buy2, sell),
				BigDecimal.valueOf(1300), BigDecimal.valueOf(1250));

		assertThat(position).isNotNull();
		assertThat(position.getQuantity()).isEqualTo(15);
		// WAC = (10000 + 12000 - 5*1100) / 15 = 16500 / 15 = 1100
		assertThat(position.getAverageCost().raw()).isEqualByComparingTo("1100.00");
		assertThat(position.getMarketValue().raw()).isEqualByComparingTo("19500.00");
		assertThat(position.getTotalReturnPercentage()).isEqualByComparingTo("18.18");
	}

	@Test
	void shouldReturnNullWhenPositionIsClosed() {
		Order buy = Order.newMarketBuy(null, null, 10, BigDecimal.valueOf(1000), LocalDateTime.now());
		Order sell = Order.newMarketSell(null, null, 10, BigDecimal.valueOf(1200), LocalDateTime.now());

		Position position = Position.fromOrders(1, "TEST", "Test", List.of(buy, sell),
				BigDecimal.valueOf(1200), BigDecimal.valueOf(1100));

		assertThat(position).isNull();
	}

	@Test
	void shouldHandleMissingMarketData() {
		Order buy = Order.newMarketBuy(null, null, 10, BigDecimal.valueOf(1000), LocalDateTime.now());

		Position position = Position.fromOrders(1, "TEST", "Test", List.of(buy), null, null);

		assertThat(position).isNotNull();
		assertThat(position.getMarketPrice()).isNull();
		assertThat(position.getMarketValue()).isNull();
		assertThat(position.getDailyReturnPercentage()).isNull();
		assertThat(position.getTotalReturnPercentage()).isNull();
	}
}
