package com.agavide.financialapi.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Integer> {

	@Query("""
		SELECT md FROM MarketData md
		WHERE md.instrument.id = :instrumentId
		ORDER BY md.date DESC, md.id DESC
		LIMIT 1
		""")
	Optional<MarketData> findLatestByInstrumentId(@Param("instrumentId") Integer instrumentId);
}
