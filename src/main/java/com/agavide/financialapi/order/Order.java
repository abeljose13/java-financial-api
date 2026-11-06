package com.agavide.financialapi.order;

import com.agavide.financialapi.asset.Instrument;
import com.agavide.financialapi.shared.domain.ConflictException;
import com.agavide.financialapi.shared.domain.Money;
import com.agavide.financialapi.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "instrumentid", referencedColumnName = "id")
	private Instrument instrument;

	@ManyToOne
	@JoinColumn(name = "userid", referencedColumnName = "id")
	private User user;

	@Column(name = "size")
	private Integer size;

	@Column(name = "price", precision = 10, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(name = "type")
	private OrderType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "side")
	private OrderSide side;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private OrderStatus status;

	@Column(name = "datetime")
	private LocalDateTime datetime;

	protected Order() {
	}

	private Order(Instrument instrument, User user, Integer size, BigDecimal price,
				  OrderType type, OrderSide side, OrderStatus status, LocalDateTime datetime) {
		this.instrument = instrument;
		this.user = user;
		this.size = size;
		this.price = price;
		this.type = type;
		this.side = side;
		this.status = status;
		this.datetime = datetime;
	}

	/**
	 * Single factory for trade orders. The initial status is decided by the
	 * {@link OrderTypeStrategy} that owns the order type (MARKET -> FILLED, LIMIT -> NEW),
	 * or set to REJECTED for business rejections.
	 */
	public static Order create(Instrument instrument, User user, int size, BigDecimal price,
							   OrderType type, OrderSide side, OrderStatus status, LocalDateTime datetime) {
		return new Order(instrument, user, size, price, type, side, status, datetime);
	}

	public static Order newCashIn(Instrument currency, User user, int amount, LocalDateTime datetime) {
		return new Order(currency, user, amount, BigDecimal.ONE, OrderType.MARKET, OrderSide.CASH_IN, OrderStatus.FILLED, datetime);
	}

	public Money totalAmount() {
		return Money.of(price.multiply(BigDecimal.valueOf(size)));
	}

	public boolean isFilled() {
		return status == OrderStatus.FILLED;
	}

	public boolean isNew() {
		return status == OrderStatus.NEW;
	}

	public boolean isRejected() {
		return status == OrderStatus.REJECTED;
	}

	public boolean isCancelled() {
		return status == OrderStatus.CANCELLED;
	}

	public boolean isBuy() {
		return side == OrderSide.BUY;
	}

	public boolean isSell() {
		return side == OrderSide.SELL;
	}

	public boolean isCashMovement() {
		return side == OrderSide.CASH_IN || side == OrderSide.CASH_OUT;
	}

	public boolean affectsPosition() {
		return isFilled() && (isBuy() || isSell());
	}

	public boolean affectsCash() {
		return isFilled() && (isCashMovement() || isBuy() || isSell());
	}

	public void cancel() {
		if (!isNew()) {
			throw new ConflictException("Order cannot be cancelled because it is " + status);
		}
		this.status = OrderStatus.CANCELLED;
	}

	public Integer getId() {
		return id;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	public User getUser() {
		return user;
	}

	public Integer getSize() {
		return size;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public OrderType getType() {
		return type;
	}

	public OrderSide getSide() {
		return side;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public LocalDateTime getDatetime() {
		return datetime;
	}
}
