package com.coremate.backend.avatar.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvatarItemRepository extends JpaRepository<AvatarItem, Long> {

    Optional<AvatarItem> findByItemIdAndActiveTrue(Long itemId);

    // 가입 시 전원에게 주는 비매품 8종. 랜덤박스 후보에서도 제외된다.
    List<AvatarItem> findByDefaultItemTrueAndActiveTrueOrderByItemIdAsc();
}
