package com.coremate.backend.shop.repository;

import com.coremate.backend.shop.entity.RandomBox;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RandomBoxRepository extends JpaRepository<RandomBox, Long> {

    List<RandomBox> findByActiveTrueOrderByRequiredLevelAscRandomBoxIdAsc();

    Optional<RandomBox> findByRandomBoxIdAndActiveTrue(Long randomBoxId);
}
