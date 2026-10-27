package com.coremate.backend.support.repository;

import com.coremate.backend.support.entity.Inquiry;
import com.coremate.backend.support.entity.type.InquiryStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @EntityGraph(attributePaths = "user")
    List<Inquiry> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status);
}
