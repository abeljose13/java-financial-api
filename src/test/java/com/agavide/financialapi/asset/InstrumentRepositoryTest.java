package com.agavide.financialapi.asset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InstrumentRepositoryTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private InstrumentRepository instrumentRepository;

	@Test
	void shouldFindAllInstruments() {
		List<Instrument> instruments = instrumentRepository.findAll();
		assertThat(instruments).hasSize(4);
	}

	@Test
	void shouldSearchByTicker() {
		List<Instrument> result = instrumentRepository.searchByTickerOrName("TEST");
		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getTicker()).isEqualTo("TEST");
	}

	@Test
	void shouldSearchByName() {
		List<Instrument> result = instrumentRepository.searchByTickerOrName("Other");
		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getTicker()).isEqualTo("OTHR");
	}

	@Test
	void shouldFindByTickerIgnoringCase() {
		Optional<Instrument> result = instrumentRepository.findByTickerIgnoreCase("ars");
		assertThat(result).isPresent();
		assertThat(result.get().isCurrency()).isTrue();
	}
}
