package com.coremate.backend.user.repository;

import com.coremate.backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNickname(String nickname);

    List<User> findAllByNameAndPhoneNumber(String name, String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);

    /**
     * 첫 닉네임 무료 설정. 아직 쓰지 않은 계정만 바꾸고 사용 표시를 남긴다.
     * 확인과 변경을 한 문장으로 해서 동시에 두 번 요청해도 한 번만 성공한다. 바뀐 행 수(0 또는 1)를 돌려준다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u SET u.nickname = :nickname, u.initialNicknameSet = true, u.updatedAt = :now
            WHERE u.userId = :userId AND u.initialNicknameSet = false
            """)
    int setInitialNickname(@Param("userId") Long userId, @Param("nickname") String nickname,
                           @Param("now") LocalDateTime now);
}
