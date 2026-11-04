package com.agavide.financialapi.asset;

import com.agavide.financialapi.asset.dto.AssetResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AssetService {

	private static final int DAILY_RETURN_SCALE = 2;
	private static final RoundingMode DAILY_RETURN_ROUNDING = RoundingMode.HALF_UP;

	private final InstrumentRepository instrumentRepository;
	private final MarketDataRepository marketDataRepository;

	public AssetService(InstrumentRepository instrumentRepository, MarketDataRepository marketDataRepository) {
		this.instrumentRepository = instrumentRepository;
		this.marketDataRepository = marketDataRepository;
	}

	public List<AssetResponse> searchAssets(String query) {
		String safeQuery = escapeLikePattern(query);
		List<Instrument> instruments = (safeQuery == null || safeQuery.isBlank())
				? instrumentRepository.findAll()
				: instrumentRepository.searchByTickerOrName(safeQuery);

		return instruments.stream()
				.map(this::toResponse)
				.toList();
	}

	private AssetResponse toResponse(Instrument instrument) {
		return marketDataRepository.findLatestByInstrumentId(instrument.getId())
				.map(md -> {
					BigDecimal lastPrice = md.getClose();
					BigDecimal dailyReturn = calculateDailyReturn(md.getClose(), md.getPreviousClose());
					return new AssetResponse(
							instrument.getId(),
							instrument.getTicker(),
							instrument.getName(),
							instrument.getType(),
							lastPrice,
							dailyReturn
					);
				})
				.orElseGet(() -> new AssetResponse(
						instrument.getId(),
						instrument.getTicker(),
						instrument.getName(),
						instrument.getType(),
						null,
						null
				));
	}

	private BigDecimal calculateDailyReturn(BigDecimal close, BigDecimal previousClose) {
		if (close == null || previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		return close.subtract(previousClose)
				.divide(previousClose, DAILY_RETURN_SCALE + 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(DAILY_RETURN_SCALE, DAILY_RETURN_ROUNDING);
	}

	static String escapeLikePattern(String query) {
		if (query == null || query.isBlank()) {
			return null;
		}
		return query
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}
}
