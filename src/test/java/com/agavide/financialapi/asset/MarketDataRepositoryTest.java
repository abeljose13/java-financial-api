package com.agavide.financialapi.asset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MarketDataRepositoryTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MarketDataRepository marketDataRepository;

	@Test
	void shouldFindLatestMarketData() {
		Optional<MarketData> latest = marketDataRepository.findLatestByInstrumentId(1);
		assertThat(latest).isPresent();
		assertThat(latest.get().getClose()).isEqualByComparingTo("1100.00");
		assertThat(latest.get().getPreviousClose()).isEqualByComparingTo("1050.00");
	}

	@Test
	void shouldReturnEmptyWhenNoMarketData() {
		Optional<MarketData> latest = marketDataRepository.findLatestByInstrumentId(999);
		assertThat(latest).isEmpty();
	}
}
