package com.coremate.backend.friend.repository;

import com.coremate.backend.friend.entity.FriendPoke;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendPokeRepository extends JpaRepository<FriendPoke, Long> {

    // 콕 찌르기 일일 제한 판정용. 별도 카운트 테이블을 두지 않고 생성 시각으로 센다.
    // 하루 경계가 겹치지 않도록 시작은 포함, 끝은 제외로 조회한다.
    long countByFromUser_UserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long fromUserId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    long countByFromUser_UserIdAndToUser_UserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long fromUserId, Long toUserId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    // 회원 탈퇴. 보낸 것과 받은 것을 모두 지운다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FriendPoke p where p.fromUser.userId = :userId or p.toUser.userId = :userId")
    int deleteAllInvolving(@Param("userId") Long userId);
}
