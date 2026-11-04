package com.agavide.financialapi.order;

import com.agavide.financialapi.asset.Instrument;
import com.agavide.financialapi.asset.InstrumentRepository;
import com.agavide.financialapi.asset.MarketData;
import com.agavide.financialapi.asset.MarketDataRepository;
import com.agavide.financialapi.portfolio.Holdings;
import com.agavide.financialapi.shared.api.ConflictException;
import com.agavide.financialapi.shared.api.ResourceNotFoundException;
import com.agavide.financialapi.shared.domain.DomainException;
import com.agavide.financialapi.shared.domain.Money;
import com.agavide.financialapi.user.User;
import com.agavide.financialapi.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

	private final UserRepository userRepository;
	private final InstrumentRepository instrumentRepository;
	private final OrderRepository orderRepository;
	private final MarketDataRepository marketDataRepository;

	public OrderService(UserRepository userRepository, InstrumentRepository instrumentRepository,
						OrderRepository orderRepository, MarketDataRepository marketDataRepository) {
		this.userRepository = userRepository;
		this.instrumentRepository = instrumentRepository;
		this.orderRepository = orderRepository;
		this.marketDataRepository = marketDataRepository;
	}

	@Transactional
	public Order sendOrder(SendOrderCommand command) {
		User user = userRepository.findWithLockById(command.userId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + command.userId()));

		Instrument instrument = instrumentRepository.findById(command.instrumentId())
				.orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + command.instrumentId()));

		if (instrument.isCurrency()) {
			throw new DomainException("Cannot trade currency instrument through this endpoint");
		}

		if (command.side() != OrderSide.BUY && command.side() != OrderSide.SELL) {
			throw new DomainException("Order side must be BUY or SELL");
		}

		BigDecimal price = resolvePrice(command, instrument);
		Integer size = resolveSize(command, price);

		if (size == null || size <= 0) {
			if (command.amountArs() != null && command.side() == OrderSide.BUY) {
				return persistRejected(instrument, user, 0, price, command.type(), command.side());
			}
			throw new DomainException("Quantity must be a positive integer");
		}

		List<Order> filledOrders = orderRepository.findFilledByUserIdOrderByDatetime(user.getId());
		Holdings holdings = new Holdings(filledOrders);

		boolean executable = canExecute(command.side(), instrument.getId(), size, price, holdings);

		if (!executable) {
			return persistRejected(instrument, user, size, price, command.type(), command.side());
		}

		return orderRepository.save(createExecutableOrder(command, instrument, user, size, price));
	}

	private BigDecimal resolvePrice(SendOrderCommand command, Instrument instrument) {
		if (command.type() == OrderType.LIMIT) {
			if (command.limitPrice() == null || command.limitPrice().compareTo(BigDecimal.ZERO) <= 0) {
				throw new DomainException("Limit price must be positive");
			}
			return command.limitPrice();
		}

		MarketData latest = marketDataRepository.findLatestByInstrumentId(instrument.getId()).orElse(null);
		if (latest == null) {
			return BigDecimal.ZERO;
		}
		return latest.getClose();
	}

	private Integer resolveSize(SendOrderCommand command, BigDecimal price) {
		if (command.side() == OrderSide.SELL) {
			if (command.quantity() == null) {
				throw new DomainException("Quantity is required for SELL orders");
			}
			return command.quantity();
		}

		if (command.quantity() != null && command.amountArs() != null) {
			throw new DomainException("Provide either quantity or amountArs, not both");
		}
		if (command.quantity() != null) {
			return command.quantity();
		}
		if (command.amountArs() != null) {
			if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
				return 0;
			}
			return command.amountArs().divide(price, 0, RoundingMode.FLOOR).intValueExact();
		}
		throw new DomainException("Quantity or amountArs is required for BUY orders");
	}

	private boolean canExecute(OrderSide side, Integer instrumentId, int size, BigDecimal price, Holdings holdings) {
		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
			return false;
		}
		if (side == OrderSide.BUY) {
			Money required = Money.of(price).multiply(size);
			return holdings.cash().isGreaterThanOrEqualTo(required);
		}
		return holdings.sharesOf(instrumentId) >= size;
	}

	private Order createExecutableOrder(SendOrderCommand command, Instrument instrument, User user, int size, BigDecimal price) {
		LocalDateTime now = LocalDateTime.now();
		if (command.type() == OrderType.MARKET) {
			return command.side() == OrderSide.BUY
					? Order.newMarketBuy(instrument, user, size, price, now)
					: Order.newMarketSell(instrument, user, size, price, now);
		}
		return command.side() == OrderSide.BUY
				? Order.newLimitBuy(instrument, user, size, price, now)
				: Order.newLimitSell(instrument, user, size, price, now);
	}

	private Order persistRejected(Instrument instrument, User user, int size, BigDecimal price,
								  OrderType type, OrderSide side) {
		return orderRepository.save(Order.newRejected(instrument, user, size, price, type, side, LocalDateTime.now()));
	}

	@Transactional
	public Order cancelOrder(Integer userId, Integer orderId) {
		Order order = orderRepository.findByIdAndUserIdWithLock(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId + " for user: " + userId));

		if (!order.isNew()) {
			throw new ConflictException("Order cannot be cancelled because it is " + order.getStatus());
		}

		order.cancel();
		return orderRepository.save(order);
	}
}
