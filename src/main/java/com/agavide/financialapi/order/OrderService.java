package com.agavide.financialapi.order;

import com.agavide.financialapi.asset.Instrument;
import com.agavide.financialapi.asset.InstrumentRepository;
import com.agavide.financialapi.portfolio.Holdings;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

	private final UserRepository userRepository;
	private final InstrumentRepository instrumentRepository;
	private final OrderRepository orderRepository;
	private final Map<OrderType, OrderTypeStrategy> strategies;

	public OrderService(UserRepository userRepository, InstrumentRepository instrumentRepository,
						OrderRepository orderRepository, List<OrderTypeStrategy> typeStrategies) {
		this.userRepository = userRepository;
		this.instrumentRepository = instrumentRepository;
		this.orderRepository = orderRepository;
		this.strategies = new EnumMap<>(OrderType.class);
		for (OrderTypeStrategy strategy : typeStrategies) {
			this.strategies.put(strategy.type(), strategy);
		}
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

		OrderTypeStrategy strategy = strategyFor(command.type());
		BigDecimal price = strategy.resolvePrice(command, instrument);
		int size = resolveSize(command, price);

		OrderStatus status = size > 0 && isExecutable(command.side(), instrument.getId(), size, price, user.getId())
				? strategy.initialStatus()
				: OrderStatus.REJECTED;

		return orderRepository.save(Order.create(instrument, user, size, price,
				command.type(), command.side(), status, LocalDateTime.now()));
	}

	@Transactional
	public Order cancelOrder(Integer userId, Integer orderId) {
		Order order = orderRepository.findByIdAndUserIdWithLock(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId + " for user: " + userId));

		order.cancel();
		return orderRepository.save(order);
	}

	private OrderTypeStrategy strategyFor(OrderType type) {
		OrderTypeStrategy strategy = strategies.get(type);
		if (strategy == null) {
			throw new DomainException("Unsupported order type: " + type);
		}
		return strategy;
	}

	/**
	 * Returns the number of shares for the request. A BUY by amount may resolve to 0 shares,
	 * which the caller persists as REJECTED (decisions.md D-4).
	 */
	private int resolveSize(SendOrderCommand command, BigDecimal price) {
		if (command.side() == OrderSide.SELL) {
			if (command.quantity() == null) {
				throw new DomainException("Quantity is required for SELL orders");
			}
			return requirePositive(command.quantity());
		}

		if (command.quantity() != null && command.amountArs() != null) {
			throw new DomainException("Provide either quantity or amountArs, not both");
		}
		if (command.quantity() != null) {
			return requirePositive(command.quantity());
		}
		if (command.amountArs() == null) {
			throw new DomainException("Quantity or amountArs is required for BUY orders");
		}
		return sharesFromAmount(command.amountArs(), price);
	}

	private static int requirePositive(int quantity) {
		if (quantity <= 0) {
			throw new DomainException("Quantity must be a positive integer");
		}
		return quantity;
	}

	private static int sharesFromAmount(BigDecimal amount, BigDecimal price) {
		if (amount.signum() <= 0) {
			throw new DomainException("amountArs must be positive");
		}
		if (price.signum() == 0) {
			return 0;
		}
		try {
			return amount.divide(price, 0, RoundingMode.FLOOR).intValueExact();
		} catch (ArithmeticException ex) {
			throw new DomainException("amountArs is too large");
		}
	}

	private boolean isExecutable(OrderSide side, Integer instrumentId, int size, BigDecimal price, Integer userId) {
		if (price.signum() <= 0) {
			return false;
		}
		Holdings holdings = new Holdings(orderRepository.findFilledByUserIdOrderByDatetime(userId));
		if (side == OrderSide.BUY) {
			return holdings.cash().isGreaterThanOrEqualTo(Money.of(price).multiply(size));
		}
		return holdings.sharesOf(instrumentId) >= size;
	}
}
