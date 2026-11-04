package com.coremate.backend.notification.repository;

import com.coremate.backend.notification.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUser_UserIdAndActiveTrue(Long userId);

    Optional<DeviceToken> findByUser_UserIdAndDeviceId(Long userId, String deviceId);

    Optional<DeviceToken> findByToken(String token);

    // 회원 탈퇴. 로그아웃처럼 끄지 않고 지운다(기기 토큰도 기기를 알아볼 수 있는 값이다).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DeviceToken t where t.user.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
