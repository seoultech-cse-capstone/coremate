package com.coremate.backend.avatar.room;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoomPlacementRepository extends JpaRepository<UserRoomPlacement, Long> {

    // 내 방·친구 방 조회. 가구 이름과 metadata.footprint 까지 한 번에 끌어온다.
    @EntityGraph(attributePaths = {"userItem", "userItem.item"})
    List<UserRoomPlacement> findByUser_UserIdOrderByGridYAscGridXAsc(Long userId);
}
