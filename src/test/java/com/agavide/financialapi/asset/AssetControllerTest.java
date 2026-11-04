package com.agavide.financialapi.asset;

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
class AssetControllerTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldReturnAllAssetsWhenNoQuery() throws Exception {
		mockMvc.perform(get("/api/v1/assets"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4)))
				.andExpect(jsonPath("$[*].ticker").value(org.hamcrest.Matchers.containsInAnyOrder("ARS", "TEST", "OTHR", "NOMD")));
	}

	@Test
	void shouldSearchByTicker() throws Exception {
		mockMvc.perform(get("/api/v1/assets?query=TEST"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].ticker").value("TEST"))
				.andExpect(jsonPath("$[0].lastPrice").value(1100.0))
				.andExpect(jsonPath("$[0].dailyReturnPercentage").value(4.76));
	}

	@Test
	void shouldSearchByNameCaseInsensitive() throws Exception {
		mockMvc.perform(get("/api/v1/assets?query=other"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].ticker").value("OTHR"));
	}

	@Test
	void shouldReturnEmptyListWhenNoMatch() throws Exception {
		mockMvc.perform(get("/api/v1/assets?query=XYZ"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}
}
