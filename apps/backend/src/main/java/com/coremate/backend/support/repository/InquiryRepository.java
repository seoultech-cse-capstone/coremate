package com.coremate.backend.support.repository;

import com.coremate.backend.support.entity.Inquiry;
import com.coremate.backend.support.entity.type.InquiryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @EntityGraph(attributePaths = "user")
    List<Inquiry> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status);

    @EntityGraph(attributePaths = "user")
    List<Inquiry> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<Inquiry> findByInquiryIdAndUserUserId(Long inquiryId, Long userId);

    // 회원 탈퇴.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Inquiry i where i.user.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
