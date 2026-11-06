package com.agavide.financialapi.order;

import com.agavide.financialapi.order.dto.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/reset-orders.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderControllerTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
	}

	@Test
	void shouldExecuteMarketBuy() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.MARKET, OrderSide.BUY, 5, null, null);

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.size").value(5))
				.andExpect(jsonPath("$.price").value(1100.0));

		// Cash impact: 80_000 - 5_500 = 74_500
		mockMvc.perform(get("/api/v1/users/1/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cashBalance").value(74500.0))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'TEST')].quantity").value(15));
	}

	@Test
	void shouldExecuteMarketSell() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.MARKET, OrderSide.SELL, 3, null, null);

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.size").value(3))
				.andExpect(jsonPath("$.price").value(1100.0));

		mockMvc.perform(get("/api/v1/users/1/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cashBalance").value(83300.0))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'TEST')].quantity").value(7));
	}

	@Test
	void shouldCreateLimitBuyAsNewWithoutImpact() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.LIMIT, OrderSide.BUY, 2, null, BigDecimal.valueOf(900));

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.price").value(900.0));

		mockMvc.perform(get("/api/v1/users/1/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cashBalance").value(80000.0))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'TEST')].quantity").value(10));
	}

	@Test
	void shouldNormalizeLimitPriceToTwoDecimals() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.LIMIT, OrderSide.BUY, 1, null, new BigDecimal("900.005"));

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.price").value(900.01));
	}

	@Test
	void shouldRejectBuyWithInsufficientFunds() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.MARKET, OrderSide.BUY, 100, null, null);

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.status").value("REJECTED"));

		mockMvc.perform(get("/api/v1/users/1/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cashBalance").value(80000.0));
	}

	@Test
	void shouldRejectSellWithInsufficientShares() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.MARKET, OrderSide.SELL, 100, null, null);

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void shouldBuyByAmountArsAndFloorShares() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.MARKET, OrderSide.BUY, null, BigDecimal.valueOf(5500), null);

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.size").value(5));
	}

	@Test
	void shouldRejectBuyWhenAmountArsConvertsToZeroShares() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.MARKET, OrderSide.BUY, null, BigDecimal.valueOf(1000), null);

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.size").value(0));
	}

	@Test
	void shouldReturn400AndNotPersistNegativeAmountArs() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.MARKET, OrderSide.BUY, null, BigDecimal.valueOf(-100), null);

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectMarketOrderWhenNoMarketData() throws Exception {
		OrderRequest request = new OrderRequest(4, OrderType.MARKET, OrderSide.BUY, 1, null, null);

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void shouldReturn404ForUnknownUser() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.MARKET, OrderSide.BUY, 1, null, null);

		mockMvc.perform(post("/api/v1/users/999/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldReturn400ForCurrencyInstrument() throws Exception {
		OrderRequest request = new OrderRequest(3, OrderType.MARKET, OrderSide.BUY, 1, null, null);

		mockMvc.perform(post("/api/v1/users/1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldNotDoubleSpendOnConcurrentMarketBuys() throws Exception {
		OrderRequest request = new OrderRequest(1, OrderType.MARKET, OrderSide.BUY, 45, null, null);
		String body = objectMapper.writeValueAsString(request);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch latch = new CountDownLatch(2);
		int[] statuses = new int[2];

		for (int i = 0; i < 2; i++) {
			final int idx = i;
			executor.submit(() -> {
				try {
					statuses[idx] = mockMvc.perform(post("/api/v1/users/2/orders")
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
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

		long createdCount = java.util.Arrays.stream(statuses).filter(s -> s == 201).count();
		long rejectedCount = java.util.Arrays.stream(statuses).filter(s -> s == 422).count();

		assertThat(createdCount).isEqualTo(1);
		assertThat(rejectedCount).isEqualTo(1);

		mockMvc.perform(get("/api/v1/users/2/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cashBalance").value(500.0))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'TEST')].quantity").value(45));
	}
}
