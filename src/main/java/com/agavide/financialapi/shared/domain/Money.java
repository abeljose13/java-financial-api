package com.agavide.financialapi.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) {

	private static final int SCALE = 2;
	private static final RoundingMode PRESENTATION_ROUNDING = RoundingMode.HALF_UP;

	public Money {
		Objects.requireNonNull(amount, "amount must not be null");
	}

	public static Money zero() {
		return new Money(BigDecimal.ZERO.setScale(SCALE));
	}

	public static Money of(BigDecimal amount) {
		return new Money(amount.setScale(SCALE, PRESENTATION_ROUNDING));
	}

	public static Money of(long amount) {
		return new Money(BigDecimal.valueOf(amount).setScale(SCALE));
	}

	public Money add(Money other) {
		return new Money(this.amount.add(other.amount));
	}

	public Money subtract(Money other) {
		return new Money(this.amount.subtract(other.amount));
	}

	public Money multiply(int quantity) {
		return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)));
	}

	public boolean isGreaterThan(Money other) {
		return this.amount.compareTo(other.amount) > 0;
	}

	public boolean isGreaterThanOrEqualTo(Money other) {
		return this.amount.compareTo(other.amount) >= 0;
	}

	public boolean isLessThan(Money other) {
		return this.amount.compareTo(other.amount) < 0;
	}

	public boolean isNegative() {
		return this.amount.compareTo(BigDecimal.ZERO) < 0;
	}

	public Money abs() {
		return new Money(this.amount.abs());
	}

	public BigDecimal raw() {
		return amount;
	}
}
