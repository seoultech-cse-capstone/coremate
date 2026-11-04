package com.coremate.backend.bookmark.repository;

import com.coremate.backend.bookmark.entity.Bookmark;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUser_UserIdAndQuestion_QuestionId(Long userId, Long questionId);

    Optional<Bookmark> findByBookmarkIdAndUser_UserId(Long bookmarkId, Long userId);

    // 내 북마크 목록은 비활성화된 문제를 제외하고 최신순으로 보여준다.
    @Query("""
            select b from Bookmark b
            join fetch b.question q
            where b.user.userId = :userId
              and q.active = true
            order by b.createdAt desc
            """)
    List<Bookmark> findActiveBookmarksByUserId(@Param("userId") Long userId);

    // 문제 목록 화면은 question.concept까지 필요해서 함께 가져온다.
    @Query("""
            select b from Bookmark b
            join fetch b.question q
            join fetch q.concept c
            where b.user.userId = :userId
              and q.active = true
            order by b.createdAt desc
            """)
    List<Bookmark> findActiveBookmarkedQuestionsByUserId(@Param("userId") Long userId);

    // 회원 탈퇴. 즐겨찾기와 거기 적은 메모를 지운다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Bookmark b where b.user.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
