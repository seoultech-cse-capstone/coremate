package com.coremate.backend.avatar.catalog;

import com.coremate.backend.avatar.catalog.AvatarItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvatarItemRepository extends JpaRepository<AvatarItem, Long> {

    Optional<AvatarItem> findByItemIdAndActiveTrue(Long itemId);
}
