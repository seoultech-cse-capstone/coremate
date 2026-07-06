package com.coremate.backend.shop.repository;

import com.coremate.backend.shop.entity.RandomBoxItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RandomBoxItemRepository extends JpaRepository<RandomBoxItem, Long> {

    @EntityGraph(attributePaths = {"randomBox", "item"})
    List<RandomBoxItem> findByRandomBox_RandomBoxIdOrderByRandomBoxItemIdAsc(Long randomBoxId);
}
