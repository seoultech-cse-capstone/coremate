package com.coremate.backend.auth.repository;

import com.coremate.backend.auth.entity.UserSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /** 인증 필터가 매 요청 쓰는 조회. uq_user_sessions_session_id 를 탄다. */
    Optional<UserSession> findBySessionId(String sessionId);

    /** 재로그인 시 재사용할 행. uq_user_sessions_user_id_device_id 를 탄다. */
    Optional<UserSession> findByUserIdAndDeviceId(Long userId, String deviceId);

    /** 기기 목록과 상한 축출에 함께 쓴다. 최근 접속 순. */
    List<UserSession> findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(Long userId);

    List<UserSession> findByUserId(Long userId);

    // 회원 탈퇴. 기기 목록에 남는 기기 정보까지 지운다. 세션 행이 없으면 남은 토큰은 SESSION_EXPIRED 로 막힌다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserSession s where s.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
