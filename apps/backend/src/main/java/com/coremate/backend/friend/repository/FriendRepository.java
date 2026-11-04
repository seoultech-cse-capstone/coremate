package com.coremate.backend.friend.repository;

import com.coremate.backend.friend.dto.type.FriendStatus;
import com.coremate.backend.friend.entity.Friend;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    @EntityGraph(attributePaths = {"requester", "receiver"})
    Optional<Friend> findByUserLowIdAndUserHighId(Long userLowId, Long userHighId);

    @EntityGraph(attributePaths = {"requester", "receiver"})
    Optional<Friend> findByFriendIdAndReceiver_UserId(Long friendId, Long receiverId);

    @EntityGraph(attributePaths = {"requester", "receiver"})
    Optional<Friend> findByFriendIdAndStatus(Long friendId, FriendStatus status);

    @EntityGraph(attributePaths = {"requester", "receiver"})
    @Query("""
            select f
            from Friend f
            where (f.requester.userId = :userId or f.receiver.userId = :userId)
              and f.status = :status
            order by f.acceptedAt desc, f.updatedAt desc
            """)
    List<Friend> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") FriendStatus status);

    @EntityGraph(attributePaths = {"requester", "receiver"})
    List<Friend> findByReceiver_UserIdAndStatusOrderByCreatedAtDesc(Long receiverId, FriendStatus status);

    @EntityGraph(attributePaths = {"requester", "receiver"})
    List<Friend> findByRequester_UserIdAndStatusInOrderByCreatedAtDesc(
            Long requesterId, Collection<FriendStatus> statuses);

    // 차단 목록. 차단하면 requester 가 차단한 사람으로 바뀌고 updated_at 이 차단 시각이 된다.
    @EntityGraph(attributePaths = {"requester", "receiver"})
    List<Friend> findByRequester_UserIdAndStatusOrderByUpdatedAtDesc(Long requesterId, FriendStatus status);

    @Query("""
            select count(f) > 0
            from Friend f
            where f.userLowId = :userLowId
              and f.userHighId = :userHighId
              and f.status = :status
            """)
    boolean existsPairByStatus(
            @Param("userLowId") Long userLowId,
            @Param("userHighId") Long userHighId,
            @Param("status") FriendStatus status
    );

    // 회원 탈퇴. 친구 관계·친구 요청·차단은 한 행에 두 회원이 함께 있어, 어느 쪽이든 이 회원이면 지운다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Friend f where f.requester.userId = :userId or f.receiver.userId = :userId")
    int deleteAllInvolving(@Param("userId") Long userId);
}
