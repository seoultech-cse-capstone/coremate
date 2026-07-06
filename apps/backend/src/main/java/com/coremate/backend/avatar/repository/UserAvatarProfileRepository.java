package com.coremate.backend.avatar.repository;

import com.coremate.backend.avatar.entity.UserAvatarProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarProfileRepository extends JpaRepository<UserAvatarProfile, Long> {

    Optional<UserAvatarProfile> findByUser_UserId(Long userId);
}
