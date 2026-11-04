package com.agavide.financialapi.asset;

import com.agavide.financialapi.asset.dto.AssetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

	@Mock
	private InstrumentRepository instrumentRepository;

	@Mock
	private MarketDataRepository marketDataRepository;

	@InjectMocks
	private AssetService assetService;

	@Test
	void shouldReturnAllInstrumentsWhenQueryIsBlank() {
		Instrument instrument = new Instrument("TEST", "Test Company", "ACCIONES");
		when(instrumentRepository.findAll()).thenReturn(List.of(instrument));
		when(marketDataRepository.findLatestByInstrumentId(instrument.getId())).thenReturn(Optional.empty());

		List<AssetResponse> result = assetService.searchAssets("  ");

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().ticker()).isEqualTo("TEST");
	}

	@Test
	void shouldSearchByTickerAndAttachLatestPrice() {
		Instrument instrument = new Instrument("TEST", "Test Company", "ACCIONES");
		//instrument.getId(); // id is null because not persisted; test uses null id
		MarketData marketData = new MarketData(instrument, null, null, null,
				BigDecimal.valueOf(1100), BigDecimal.valueOf(1050), LocalDate.now());

		when(instrumentRepository.searchByTickerOrName("TEST")).thenReturn(List.of(instrument));
		when(marketDataRepository.findLatestByInstrumentId(null)).thenReturn(Optional.of(marketData));

		List<AssetResponse> result = assetService.searchAssets("TEST");

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().lastPrice()).isEqualByComparingTo("1100");
		assertThat(result.getFirst().dailyReturnPercentage()).isEqualByComparingTo("4.76");
	}

	@Test
	void shouldEscapeLikeSpecialCharacters() {
		assertThat(AssetService.escapeLikePattern("100%")).isEqualTo("100\\%");
		assertThat(AssetService.escapeLikePattern("A_B")).isEqualTo("A\\_B");
	}
}
