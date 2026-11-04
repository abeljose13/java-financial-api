package com.agavide.financialapi.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Integer> {

	@Query("""
		SELECT i FROM Instrument i
		WHERE LOWER(i.ticker) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
		   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
		ORDER BY i.ticker
		""")
	List<Instrument> searchByTickerOrName(@Param("query") String query);

	Optional<Instrument> findByTickerIgnoreCase(String ticker);
}
