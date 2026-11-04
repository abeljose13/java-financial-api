package com.agavide.financialapi.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

	@Query("""
		SELECT o FROM Order o
		WHERE o.user.id = :userId
		ORDER BY o.datetime ASC, o.id ASC
		""")
	List<Order> findAllByUserIdOrderByDatetime(@Param("userId") Integer userId);

	@Query("""
		SELECT o FROM Order o
		WHERE o.user.id = :userId
		  AND o.status = com.agavide.financialapi.order.OrderStatus.FILLED
		ORDER BY o.datetime ASC, o.id ASC
		""")
	List<Order> findFilledByUserIdOrderByDatetime(@Param("userId") Integer userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		SELECT o FROM Order o
		WHERE o.id = :orderId
		  AND o.user.id = :userId
		""")
	Optional<Order> findByIdAndUserIdWithLock(@Param("orderId") Integer orderId, @Param("userId") Integer userId);
}
