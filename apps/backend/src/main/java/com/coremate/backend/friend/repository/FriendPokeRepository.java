package com.coremate.backend.friend.repository;

import com.coremate.backend.friend.entity.FriendPoke;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendPokeRepository extends JpaRepository<FriendPoke, Long> {

    @EntityGraph(attributePaths = {"fromUser", "toUser"})
    List<FriendPoke> findByToUser_UserIdOrderByCreatedAtDesc(Long toUserId);

    @EntityGraph(attributePaths = {"fromUser", "toUser"})
    List<FriendPoke> findByToUser_UserIdAndReadFalseOrderByCreatedAtDesc(Long toUserId);

    @EntityGraph(attributePaths = {"fromUser", "toUser"})
    Optional<FriendPoke> findByPokeIdAndToUser_UserId(Long pokeId, Long toUserId);
}
