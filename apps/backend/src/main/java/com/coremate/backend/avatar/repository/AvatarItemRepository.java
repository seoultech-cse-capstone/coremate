package com.coremate.backend.avatar.repository;

import com.coremate.backend.avatar.entity.AvatarItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvatarItemRepository extends JpaRepository<AvatarItem, Long> {

    Optional<AvatarItem> findByItemIdAndActiveTrue(Long itemId);
}
