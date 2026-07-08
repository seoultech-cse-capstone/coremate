package com.coremate.backend.reward.repository;

import com.coremate.backend.reward.entity.UserCurrency;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCurrencyRepository extends JpaRepository<UserCurrency, Long> {

    // 코인 지급/차감 시 같은 사용자 잔액을 동시에 수정하지 못하게 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select uc from UserCurrency uc where uc.userId = :userId")
    Optional<UserCurrency> findByUserIdForUpdate(@Param("userId") Long userId);
}
