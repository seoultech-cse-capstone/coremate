package com.coremate.backend.notification.repository;

import com.coremate.backend.notification.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUser_UserIdAndActiveTrue(Long userId);

    Optional<DeviceToken> findByUser_UserIdAndDeviceId(Long userId, String deviceId);

    Optional<DeviceToken> findByToken(String token);
}
