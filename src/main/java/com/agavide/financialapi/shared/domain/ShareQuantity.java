package com.agavide.financialapi.shared.domain;

public record ShareQuantity(int value) {

	public ShareQuantity {
		if (value < 0) {
			throw new IllegalArgumentException("share quantity cannot be negative");
		}
	}

	public static ShareQuantity zero() {
		return new ShareQuantity(0);
	}

	public ShareQuantity add(ShareQuantity other) {
		return new ShareQuantity(this.value + other.value);
	}

	public ShareQuantity subtract(ShareQuantity other) {
		return new ShareQuantity(this.value - other.value);
	}

	public boolean isGreaterThan(ShareQuantity other) {
		return this.value > other.value;
	}

	public boolean isGreaterThanOrEqualTo(ShareQuantity other) {
		return this.value >= other.value;
	}

	public boolean isZero() {
		return this.value == 0;
	}
}
