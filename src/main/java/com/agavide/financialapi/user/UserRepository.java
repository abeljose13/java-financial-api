package com.agavide.financialapi.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<User> findWithLockById(Integer id);
}
