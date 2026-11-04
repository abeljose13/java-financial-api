package com.agavide.financialapi.portfolio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioControllerTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldReturnPortfolioForUserWithPositions() throws Exception {
		mockMvc.perform(get("/api/v1/users/1/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.cashBalance").value(80000.0))
				.andExpect(jsonPath("$.availableCash").value(80000.0))
				.andExpect(jsonPath("$.totalAccountValue").value(101500.0))
				.andExpect(jsonPath("$.positions", hasSize(2)))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'TEST')].quantity").value(10))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'TEST')].averageCost").value(1000.0))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'TEST')].marketValue").value(11000.0))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'TEST')].totalReturnPercentage").value(10.0))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'OTHR')].quantity").value(5))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'OTHR')].marketValue").value(10500.0))
				.andExpect(jsonPath("$.positions[?(@.ticker == 'OTHR')].totalReturnPercentage").value(5.0));
	}

	@Test
	void shouldReturnPortfolioForUserWithOnlyCash() throws Exception {
		mockMvc.perform(get("/api/v1/users/2/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(2))
				.andExpect(jsonPath("$.cashBalance").value(50000.0))
				.andExpect(jsonPath("$.totalAccountValue").value(50000.0))
				.andExpect(jsonPath("$.positions", hasSize(0)));
	}

	@Test
	void shouldReturn404ForUnknownUser() throws Exception {
		mockMvc.perform(get("/api/v1/users/999/portfolio"))
				.andExpect(status().isNotFound());
	}
}
