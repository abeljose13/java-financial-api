package com.agavide.financialapi.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/reset-orders.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderCancellationControllerTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldCancelNewOrder() throws Exception {
		mockMvc.perform(delete("/api/v1/users/1/orders/4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value(4))
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(get("/api/v1/users/1/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cashBalance").value(80000.0))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'TEST')].quantity").value(10));
	}

	@Test
	void shouldReturn409WhenCancellingAlreadyCancelledOrder() throws Exception {
		mockMvc.perform(delete("/api/v1/users/1/orders/4"))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/api/v1/users/1/orders/4"))
				.andExpect(status().isConflict());
	}

	@Test
	void shouldReturn409WhenCancellingFilledOrder() throws Exception {
		mockMvc.perform(delete("/api/v1/users/1/orders/1"))
				.andExpect(status().isConflict());
	}

	@Test
	void shouldReturn404WhenOrderBelongsToAnotherUser() throws Exception {
		mockMvc.perform(delete("/api/v1/users/2/orders/4"))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldReturn404ForUnknownOrder() throws Exception {
		mockMvc.perform(delete("/api/v1/users/1/orders/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldAllowOnlyOneConcurrentCancellation() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch latch = new CountDownLatch(2);
		int[] statuses = new int[2];

		for (int i = 0; i < 2; i++) {
			final int idx = i;
			executor.submit(() -> {
				try {
					statuses[idx] = mockMvc.perform(delete("/api/v1/users/1/orders/4"))
							.andReturn()
							.getResponse()
							.getStatus();
				} catch (Exception e) {
					statuses[idx] = -1;
				} finally {
					latch.countDown();
				}
			});
		}

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		executor.shutdown();

		long okCount = java.util.Arrays.stream(statuses).filter(s -> s == 200).count();
		long conflictCount = java.util.Arrays.stream(statuses).filter(s -> s == 409).count();

		assertThat(okCount).isEqualTo(1);
		assertThat(conflictCount).isEqualTo(1);
	}
}
