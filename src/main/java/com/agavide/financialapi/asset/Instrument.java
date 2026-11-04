package com.agavide.financialapi.asset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instruments")
public class Instrument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@Column(name = "ticker")
	private String ticker;

	@Column(name = "name")
	private String name;

	@Column(name = "type")
	private String type;

	protected Instrument() {
	}

	public Instrument(String ticker, String name, String type) {
		this.ticker = ticker;
		this.name = name;
		this.type = type;
	}

	public Integer getId() {
		return id;
	}

	public String getTicker() {
		return ticker;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public boolean isCurrency() {
		return "MONEDA".equalsIgnoreCase(type);
	}
}
