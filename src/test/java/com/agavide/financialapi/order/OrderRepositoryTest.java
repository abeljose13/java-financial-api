package com.agavide.financialapi.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderRepositoryTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private OrderRepository orderRepository;

	@Test
	void shouldFindFilledOrdersByUser() {
		List<Order> filled = orderRepository.findFilledByUserIdOrderByDatetime(1);
		assertThat(filled).hasSize(3);
		assertThat(filled).allMatch(Order::isFilled);
	}

	@Test
	void shouldMapEnumsAsStrings() {
		List<Order> orders = orderRepository.findAll().stream()
				.filter(order -> order.getUser().getId() == 1)
				.toList();
		long newCount = orders.stream().filter(Order::isNew).count();
		long rejectedCount = orders.stream().filter(Order::isRejected).count();
		long cancelledCount = orders.stream().filter(Order::isCancelled).count();

		assertThat(orders).hasSize(6);
		assertThat(newCount).isEqualTo(1);
		assertThat(rejectedCount).isEqualTo(1);
		assertThat(cancelledCount).isEqualTo(1);
	}
}
