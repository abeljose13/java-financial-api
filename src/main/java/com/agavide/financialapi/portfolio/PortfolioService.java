package com.agavide.financialapi.portfolio;

import com.agavide.financialapi.asset.Instrument;
import com.agavide.financialapi.asset.InstrumentRepository;
import com.agavide.financialapi.asset.MarketData;
import com.agavide.financialapi.asset.MarketDataRepository;
import com.agavide.financialapi.order.Order;
import com.agavide.financialapi.order.OrderRepository;
import com.agavide.financialapi.shared.api.ResourceNotFoundException;
import com.agavide.financialapi.shared.domain.Money;
import com.agavide.financialapi.user.User;
import com.agavide.financialapi.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final InstrumentRepository instrumentRepository;
	private final MarketDataRepository marketDataRepository;

	public PortfolioService(UserRepository userRepository, OrderRepository orderRepository,
							InstrumentRepository instrumentRepository, MarketDataRepository marketDataRepository) {
		this.userRepository = userRepository;
		this.orderRepository = orderRepository;
		this.instrumentRepository = instrumentRepository;
		this.marketDataRepository = marketDataRepository;
	}

	public Portfolio getPortfolio(Integer userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

		List<Order> filledOrders = orderRepository.findFilledByUserIdOrderByDatetime(user.getId());
		Money cashBalance = new Holdings(filledOrders).cash();

		Map<Integer, List<Order>> ordersByInstrument = filledOrders.stream()
				.filter(order -> !order.getInstrument().isCurrency())
				.collect(Collectors.groupingBy(order -> order.getInstrument().getId()));

		List<Position> positions = ordersByInstrument.entrySet().stream()
				.map(entry -> buildPosition(entry.getKey(), entry.getValue()))
				.filter(Objects::nonNull)
				.toList();

		return new Portfolio(user.getId(), cashBalance, positions);
	}

	private Position buildPosition(Integer instrumentId, List<Order> orders) {
		Instrument instrument = instrumentRepository.findById(instrumentId)
				.orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + instrumentId));

		MarketData latest = marketDataRepository.findLatestByInstrumentId(instrumentId).orElse(null);

		return Position.fromOrders(
				instrumentId,
				instrument.getTicker(),
				instrument.getName(),
				orders,
				latest != null ? latest.getClose() : null,
				latest != null ? latest.getPreviousClose() : null
		);
	}
}
