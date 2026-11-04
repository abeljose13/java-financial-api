package com.agavide.financialapi.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@Column(name = "email")
	private String email;

	@Column(name = "accountnumber")
	private String accountNumber;

	protected User() {
	}

	public User(String email, String accountNumber) {
		this.email = email;
		this.accountNumber = accountNumber;
	}

	public Integer getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getAccountNumber() {
		return accountNumber;
	}
}
