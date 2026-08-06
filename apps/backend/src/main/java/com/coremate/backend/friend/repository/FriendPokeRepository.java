package com.coremate.backend.friend.repository;

import com.coremate.backend.friend.entity.FriendPoke;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendPokeRepository extends JpaRepository<FriendPoke, Long> {

    // 콕 찌르기 일일 제한 판정용. 별도 카운트 테이블을 두지 않고 생성 시각으로 센다.
    // 하루 경계가 겹치지 않도록 시작은 포함, 끝은 제외로 조회한다.
    long countByFromUser_UserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long fromUserId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    long countByFromUser_UserIdAndToUser_UserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long fromUserId, Long toUserId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    @EntityGraph(attributePaths = {"fromUser", "toUser"})
    List<FriendPoke> findByToUser_UserIdOrderByCreatedAtDesc(Long toUserId);

    @EntityGraph(attributePaths = {"fromUser", "toUser"})
    List<FriendPoke> findByToUser_UserIdAndReadFalseOrderByCreatedAtDesc(Long toUserId);

    @EntityGraph(attributePaths = {"fromUser", "toUser"})
    Optional<FriendPoke> findByPokeIdAndToUser_UserId(Long pokeId, Long toUserId);
}
