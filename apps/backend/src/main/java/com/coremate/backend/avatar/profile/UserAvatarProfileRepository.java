package com.coremate.backend.avatar.profile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarProfileRepository extends JpaRepository<UserAvatarProfile, Long> {

    Optional<UserAvatarProfile> findByUser_UserId(Long userId);

    // 친구 목록처럼 여러 사람의 프로필을 한 번에 읽는다. 사람마다 읽으면 목록 길이만큼 쿼리가 나간다.
    List<UserAvatarProfile> findByUser_UserIdIn(Collection<Long> userIds);
}
