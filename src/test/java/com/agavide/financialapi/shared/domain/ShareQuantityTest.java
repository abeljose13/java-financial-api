package com.agavide.financialapi.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShareQuantityTest {

	@Test
	void shouldAddShares() {
		ShareQuantity a = new ShareQuantity(10);
		ShareQuantity b = new ShareQuantity(5);
		assertThat(a.add(b).value()).isEqualTo(15);
	}

	@Test
	void shouldSubtractShares() {
		ShareQuantity a = new ShareQuantity(10);
		ShareQuantity b = new ShareQuantity(3);
		assertThat(a.subtract(b).value()).isEqualTo(7);
	}

	@Test
	void shouldRejectNegativeQuantity() {
		assertThatThrownBy(() -> new ShareQuantity(-1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("cannot be negative");
	}

	@Test
	void zeroQuantityIsValid() {
		assertThat(ShareQuantity.zero().value()).isZero();
	}
}
