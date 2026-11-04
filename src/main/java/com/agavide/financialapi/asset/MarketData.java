package com.agavide.financialapi.asset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "marketdata")
public class MarketData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "instrumentid", referencedColumnName = "id")
	private Instrument instrument;

	@Column(name = "high", precision = 10, scale = 2)
	private BigDecimal high;

	@Column(name = "low", precision = 10, scale = 2)
	private BigDecimal low;

	@Column(name = "open", precision = 10, scale = 2)
	private BigDecimal open;

	@Column(name = "close", precision = 10, scale = 2)
	private BigDecimal close;

	@Column(name = "previousclose", precision = 10, scale = 2)
	private BigDecimal previousClose;

	@Column(name = "date")
	private LocalDate date;

	protected MarketData() {
	}

	public MarketData(Instrument instrument, BigDecimal high, BigDecimal low, BigDecimal open,
					  BigDecimal close, BigDecimal previousClose, LocalDate date) {
		this.instrument = instrument;
		this.high = high;
		this.low = low;
		this.open = open;
		this.close = close;
		this.previousClose = previousClose;
		this.date = date;
	}

	public Integer getId() {
		return id;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	public BigDecimal getHigh() {
		return high;
	}

	public BigDecimal getLow() {
		return low;
	}

	public BigDecimal getOpen() {
		return open;
	}

	public BigDecimal getClose() {
		return close;
	}

	public BigDecimal getPreviousClose() {
		return previousClose;
	}

	public LocalDate getDate() {
		return date;
	}
}
