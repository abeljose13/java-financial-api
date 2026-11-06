package com.agavide.financialapi.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

	@Test
	void shouldAddMoney() {
		Money a = Money.of(BigDecimal.valueOf(100.50));
		Money b = Money.of(BigDecimal.valueOf(50.25));
		assertThat(a.add(b).raw()).isEqualByComparingTo(BigDecimal.valueOf(150.75));
	}

	@Test
	void shouldMultiplyByQuantity() {
		Money price = Money.of(BigDecimal.valueOf(10.50));
		assertThat(price.multiply(3).raw()).isEqualByComparingTo(BigDecimal.valueOf(31.50));
	}

	@Test
	void shouldCompareMoney() {
		Money a = Money.of(BigDecimal.valueOf(100));
		Money b = Money.of(BigDecimal.valueOf(200));
		assertThat(b.isGreaterThanOrEqualTo(a)).isTrue();
		assertThat(a.isGreaterThanOrEqualTo(a)).isTrue();
		assertThat(a.isGreaterThanOrEqualTo(b)).isFalse();
	}

	@Test
	void shouldRejectNullAmount() {
		assertThatThrownBy(() -> new Money(null))
			.isInstanceOf(NullPointerException.class);
	}
}
